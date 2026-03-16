package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.LinkStatsResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ClickAnalyticsRepository;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    @Transactional(readOnly = true)
    public LinkStatsResponse execute(String shortCode, Instant from, Instant to) {
        ShortLink link = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
            .orElseThrow(() -> new ShortLinkNotFoundException("Link not found: " + shortCode));

        // Ownership check
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail)
            .orElseThrow(() -> new AccessDeniedException("User not found"));

        if (link.getUserId() != null && !link.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view analytics for this link");
        }
        
        // If link is anonymous (userId == null), we might want to allow viewing if we have some other token, 
        // but for now, let's assume only owned links have private analytics.
        if (link.getUserId() == null) {
            throw new AccessDeniedException("Analytics are only available for links owned by a user");
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
