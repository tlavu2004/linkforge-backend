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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
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

        boolean isAuthorized = false;

        // 1. Check Ownership (if authenticated)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String currentEmail = auth.getName();
            User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
            
            if (currentUser != null && link.getUserId() != null && link.getUserId().equals(currentUser.getId())) {
                isAuthorized = true;
            }
        }

        // 2. Check Token (if not owner or owner check skipped)
        if (!isAuthorized && token != null && link.getDeleteTokenHash() != null) {
            if (passwordEncoder.matches(token, link.getDeleteTokenHash())) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            throw new AccessDeniedException("You don't have permission to view analytics for this link. Please provide a valid token or log in as the owner.");
        }

        return LinkStatsResponse.builder()
                .shortCode(shortCode)
                .totalClicks(clickAnalyticsRepository.countTotalClicks(shortCode))
                .uniqueVisitors(clickAnalyticsRepository.countUniqueVisitors(shortCode))
                .clicksByCountry(clickAnalyticsRepository.countByCountry(shortCode))
                .clicksByDeviceType(clickAnalyticsRepository.countByDeviceType(shortCode))
                .clicksByReferrer(clickAnalyticsRepository.countByReferrer(shortCode))
                .dailyStats(clickAnalyticsRepository.getDailyClickStats(shortCode, from, to))
                .build();
    }
}
