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

@Component
@RequiredArgsConstructor
@Slf4j
public class ShortLinkEventListener {

    private final ShortLinkRepository shortLinkRepository;

    @Async
    @EventListener
    @Transactional
    public void handleShortLinkAccessed(ShortLinkAccessedEvent event) {
        try {
            shortLinkRepository.incrementClickCount(ShortCode.of(event.shortCode()));
            log.debug("Incremented click count for shortCode: {}", event.shortCode());
        } catch (Exception e) {
            log.error("Failed to increment click count for shortCode: {}", event.shortCode(), e);
        }
    }
}
