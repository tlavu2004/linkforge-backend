package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.event.ShortLinkAccessedEvent;
import com.tlavu.linkforge.domain.exception.ShortLinkExpiredException;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.cache.ShortLinkCacheService;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResolveShortLinkUseCaseImpl implements ResolveShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkCacheService shortLinkCacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.tlavu.linkforge.infrastructure.metrics.MetricsService metricsService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ShortLinkResponse execute(String shortCode) {
        metricsService.incrementLinksResolved();

        // 1. Check cache
        var cachedResponse = shortLinkCacheService.getShortLink(shortCode);
        if (cachedResponse.isPresent()) {
            ShortLinkResponse response = cachedResponse.get();
            if (response.expiresAt() != null && response.expiresAt().isBefore(Instant.now())) {
                shortLinkCacheService.evictShortLink(shortCode);
                throw new ShortLinkExpiredException("Short link has expired: " + shortCode);
            }
            metricsService.incrementCacheHits();
            // Still track click even on cache hit
            eventPublisher.publishEvent(new ShortLinkAccessedEvent(shortCode, Instant.now()));
            return response;
        }

        metricsService.incrementCacheMisses();

        // 2. Query DB
        ShortCode code = ShortCode.of(shortCode);
        ShortLink shortLink = shortLinkRepository.findByShortCode(code)
                .orElseThrow(() -> new ShortLinkNotFoundException("Short link not found: " + shortCode));

        if (shortLink.isExpired(Instant.now())) {
            throw new ShortLinkExpiredException("Short link has expired: " + shortCode);
        }

        // 3. Determine skipAds
        boolean skipAds = false;
        if (shortLink.getUserId() != null) {
            Optional<User> userOpt = userRepository.findById(shortLink.getUserId());
            if (userOpt.isPresent()) {
                skipAds = userOpt.get().isVipActive(Instant.now());
            }
        }

        // 4. Create response
        ShortLinkResponse response = new ShortLinkResponse(
                shortLink.getShortCode().code(),
                shortLink.getOriginalUrl().url(),
                shortLink.getCreatedAt(),
                shortLink.getExpiresAt(),
                null, // deleteToken not returned when resolving
                skipAds);

        // 4. Save to cache
        shortLinkCacheService.saveShortLink(shortCode, response);

        // 5. Publish access event for async click tracking
        eventPublisher.publishEvent(new ShortLinkAccessedEvent(shortCode, Instant.now()));

        return response;
    }
}
