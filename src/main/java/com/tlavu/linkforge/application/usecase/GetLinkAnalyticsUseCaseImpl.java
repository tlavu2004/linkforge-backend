package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.LinkStatsResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ClickAnalyticsRepository;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class GetLinkAnalyticsUseCaseImpl implements GetLinkAnalyticsUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public LinkStatsResponse execute(String shortCode, String token, Instant from, Instant to) {
        ShortLink link = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
            .orElseThrow(() -> new ShortLinkNotFoundException("Link not found: " + shortCode));

        log.info("Checking analytics for shortCode: {}, token provided: {}", shortCode, token != null ? "YES" : "NO");

        boolean isAuthorized = false;

        // 1. Check Ownership (if authenticated)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String currentEmail = auth.getName();
            User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
            
            if (currentUser != null && link.getUserId() != null && link.getUserId().equals(currentUser.getId())) {
                isAuthorized = true;
                log.info("User {} is the owner of link {}", currentEmail, shortCode);
            }
        } else {
            log.info("No authenticated user found (or anonymous)");
        }

        // 2. Check Token (if not owner or owner check skipped)
        if (!isAuthorized && token != null && link.getDeleteTokenHash() != null) {
            log.info("Attempting token validation for link {}", shortCode);
            if (passwordEncoder.matches(token, link.getDeleteTokenHash())) {
                isAuthorized = true;
                log.info("Token validation successful for link {}", shortCode);
            } else {
                log.warn("Token validation failed for link {}", shortCode);
            }
        }

        if (!isAuthorized) {
            log.warn("Authorization failed for link {}. isAuthorized={}, tokenProvided={}, hasHash={}",
                    shortCode, isAuthorized, token != null, link.getDeleteTokenHash() != null);
            throw new AccessDeniedException("You don't have permission to view analytics for this link. Please provide a valid token or log in as the owner.");
        }

        return LinkStatsResponse.builder()
                .shortCode(shortCode)
                .totalClicks(clickAnalyticsRepository.countTotalClicks(shortCode, from, to))
                .uniqueVisitors(clickAnalyticsRepository.countUniqueVisitors(shortCode, from, to))
                .clicksByCountry(clickAnalyticsRepository.countByCountry(shortCode, from, to))
                .clicksByDeviceType(clickAnalyticsRepository.countByDeviceType(shortCode, from, to))
                .clicksByReferrer(clickAnalyticsRepository.countByReferrer(shortCode, from, to))
                .dailyStats(clickAnalyticsRepository.getDailyClickStats(shortCode, from, to))
                .build();
    }
}
