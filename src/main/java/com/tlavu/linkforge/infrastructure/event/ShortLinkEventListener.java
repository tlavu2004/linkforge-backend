package com.tlavu.linkforge.infrastructure.event;

import com.tlavu.linkforge.domain.event.ShortLinkAccessedEvent;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tlavu.linkforge.domain.entity.ClickAnalytics;
import com.tlavu.linkforge.domain.entity.DeviceType;
import com.tlavu.linkforge.domain.repository.ClickAnalyticsRepository;
import com.tlavu.linkforge.infrastructure.service.GeoLocationService;
import com.tlavu.linkforge.infrastructure.service.UserAgentParser;
import io.hypersistence.tsid.TSID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShortLinkEventListener {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final GeoLocationService geoLocationService;
    private final UserAgentParser userAgentParser;

    @Async
    @EventListener
    @Transactional
    @SuppressWarnings("null")
    public void handleShortLinkAccessed(ShortLinkAccessedEvent event) {
        try {
            // 1. Basic click count increment
            shortLinkRepository.incrementClickCount(ShortCode.of(event.shortCode()));
            
            // 2. Detailed analytics enrichment
            GeoLocationService.GeoData geoData = geoLocationService.getLocation(event.ipAddress());
            DeviceType deviceType = userAgentParser.parseDeviceType(event.userAgent());
            
            ClickAnalytics analytics = ClickAnalytics.create(
                TSID.fast().toLong(),
                event.shortCode(),
                event.accessedAt(),
                event.ipAddress(),
                event.userAgent(),
                geoData.getCountryCode(),
                geoData.getCity(),
                deviceType,
                event.referrer()
            );
            
            clickAnalyticsRepository.save(analytics);
            
            log.debug("Enriched and saved analytics for shortCode: {}", event.shortCode());
        } catch (Exception e) {
            log.error("Failed to process analytics for shortCode: {}", event.shortCode(), e);
        }
    }
}
