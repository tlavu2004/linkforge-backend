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
import com.tlavu.linkforge.infrastructure.metrics.MetricsService;
import com.tlavu.linkforge.infrastructure.security.JwtService;
import io.hypersistence.tsid.TSID;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CreateShortLinkUseCaseImpl implements CreateShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final MetricsService metricsService;
    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]+$");
    private static final Set<String> SYSTEM_RESERVED_WORDS = Set.of(
            "admin", "api", "dashboard", "login", "logout", "register",
            "static", "assets", "health", "v1", "swagger-ui", "v3",
            "analytics", "links", "users", "payments", "ads", "redirect",
            "support", "help", "auth", "oauth", "callback");

    private static final Set<String> FORBIDDEN_PHISHING_WORDS = Set.of(
            "google", "facebook", "apple", "microsoft", "paypal", "pay", "bank",
            "binance", "crypto", "verify", "verification", "account", "secure",
            "security", "official", "update", "support", "billing", "signin",
            "signup", "password", "credential", "vnpay", "momo", "zalopay");

    @Override
    @Transactional
    public ShortLinkResponse execute(CreateShortLinkCommand command) {
        OriginalUrl originalUrl = OriginalUrl.of(command.originalUrl());

        ShortCode shortCode;
        if (command.customAlias() != null && !command.customAlias().isBlank()) {
            validateCustomAlias(command.customAlias());
            shortCode = ShortCode.of(command.customAlias());
            if (shortLinkRepository.existsByShortCode(shortCode)) {
                throw new DomainException("link.alias_taken");
            }
        } else {
            shortCode = shortCodeGenerator.generate();
        }

        String deleteToken = UUID.randomUUID().toString();

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

        boolean canSetCustomExpiration = userId != null;

        if (command.expiresAt() != null && !canSetCustomExpiration) {
            throw new DomainException("vip.custom_expiration_only");
        }

        // Default 30 days expiration if not specified
        Instant expiresAt = command.expiresAt();
        if (expiresAt == null) {
            expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
        }

        ShortLink shortLink = ShortLink.create(
                TSID.fast().toLong(),
                shortCode,
                originalUrl,
                expiresAt,
                userId,
                passwordEncoder.encode(deleteToken));

        ShortLink savedLink = shortLinkRepository.save(shortLink);

        metricsService.incrementLinksCreated();

        return new ShortLinkResponse(
                savedLink.getShortCode().code(),
                savedLink.getOriginalUrl().url(),
                savedLink.getCreatedAt(),
                savedLink.getExpiresAt(),
                deleteToken, // Return raw token to user
                isVip,
                savedLink.getQrCode());
    }

    private void validateCustomAlias(String alias) {
        if (alias.length() < 3 || alias.length() > 50) {
            throw new DomainException("validation.alias_length");
        }
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new DomainException("validation.alias_invalid_chars");
        }
        String lowerAlias = alias.toLowerCase();

        // 1. Exact match for system reserved words
        if (SYSTEM_RESERVED_WORDS.contains(lowerAlias)) {
            throw new DomainException("link.alias_reserved");
        }

        // 2. Contains detection for common phishing/scam keywords
        for (String forbidden : FORBIDDEN_PHISHING_WORDS) {
            if (lowerAlias.contains(forbidden)) {
                throw new DomainException("link.alias_forbidden");
            }
        }
    }
}
