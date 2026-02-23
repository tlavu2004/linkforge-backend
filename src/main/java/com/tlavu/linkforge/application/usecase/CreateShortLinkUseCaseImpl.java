package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.domain.service.ShortCodeGenerator;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
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

    @Override
    @Transactional
    public ShortLinkResponse execute(CreateShortLinkCommand command) {
        OriginalUrl originalUrl = OriginalUrl.of(command.originalUrl());
        ShortCode shortCode = shortCodeGenerator.generate();
        String deleteToken = java.util.UUID.randomUUID().toString();

        boolean isVip = false;
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
                }
            }
        }

        if (command.expiresAt() != null && !isVip) {
            throw new DomainException("Only VIP users can set custom expiration time for short links");
        }

        ShortLink shortLink = ShortLink.create(
                io.hypersistence.tsid.TSID.fast().toLong(),
                shortCode,
                originalUrl,
                command.expiresAt(),
                deleteToken);

        ShortLink savedLink = shortLinkRepository.save(shortLink);

        metricsService.incrementLinksCreated();

        return new ShortLinkResponse(
                savedLink.getShortCode().code(),
                savedLink.getOriginalUrl().url(),
                savedLink.getCreatedAt(),
                savedLink.getExpiresAt(),
                savedLink.getDeleteTokenHash());
    }
}
