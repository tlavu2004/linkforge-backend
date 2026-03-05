package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.command.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.domain.service.ShortCodeGenerator;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateShortLinkUseCaseImpl implements CreateShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final com.tlavu.linkforge.infrastructure.metrics.MetricsService metricsService;
    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final JwtService jwtService;

    @Override
    @Transactional
    public ShortLinkResponse execute(CreateShortLinkCommand command) {
        OriginalUrl originalUrl = OriginalUrl.of(command.originalUrl());
        ShortCode shortCode = shortCodeGenerator.generate();
        String deleteToken = java.util.UUID.randomUUID().toString();

        Long userId = null;
        boolean isVip = false;

        // 1. Try to get user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = null;
            if (auth.getPrincipal() instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
            } else if (auth.getPrincipal() instanceof String str) {
                email = str;
            }
            if (email != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    isVip = userOpt.get().isVipActive(Instant.now());
                    userId = userOpt.get().getId();
                }
            }
        }

        // 2. Fallback: Try to get user from Authorization header directly (for
        // permitAll endpoints)
        if (userId == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    userId = jwtService.extractUserId(jwt);
                    if (userId != null) {
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isPresent()) {
                            isVip = userOpt.get().isVipActive(Instant.now());
                        }
                    }
                } catch (Exception e) {
                    // Invalid or expired token is ignored for optional authentication
                }
            }
        }

        boolean canSetCustomExpiration = isVip || (userId != null && userRepository.findById(userId)
                .map(u -> u.getRole() == com.tlavu.linkforge.domain.entity.Role.ADMIN).orElse(false));

        if (command.expiresAt() != null && !canSetCustomExpiration) {
            throw new DomainException("Only VIP users and Admins can set custom expiration time for short links");
        }

        // Default 30 days expiration for non-VIP / non-Admin / anonymous users
        Instant expiresAt = command.expiresAt();
        if (expiresAt == null && !canSetCustomExpiration) {
            expiresAt = Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS);
        }

        ShortLink shortLink = ShortLink.create(
                io.hypersistence.tsid.TSID.fast().toLong(),
                shortCode,
                originalUrl,
                expiresAt,
                userId,
                deleteToken);

        ShortLink savedLink = shortLinkRepository.save(shortLink);

        metricsService.incrementLinksCreated();

        return new ShortLinkResponse(
                savedLink.getShortCode().code(),
                savedLink.getOriginalUrl().url(),
                savedLink.getCreatedAt(),
                savedLink.getExpiresAt(),
                savedLink.getDeleteTokenHash(),
                isVip,
                savedLink.getQrCode());
    }
}
