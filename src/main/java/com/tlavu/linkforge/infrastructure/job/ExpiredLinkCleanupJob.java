package com.tlavu.linkforge.infrastructure.job;

import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.infrastructure.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredLinkCleanupJob {

    private final ShortLinkRepository shortLinkRepository;
    private final MetricsService metricsService;

    /**
     * Executes daily at 2:00 AM server time.
     * Hard-deletes all short links where expires_at < now.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredLinks() {
        log.info("Starting scheduled cleanup of expired short links...");
        Instant now = Instant.now();

        try {
            int deletedCount = shortLinkRepository.deleteExpiredLinks(now);
            log.info("Successfully deleted {} expired short links.", deletedCount);

            // Assuming metricsService might not have a specific method for this yet,
            // we will just log it for now. If you want to track it in Prometheus,
            // you can add `metricsService.incrementLinksDeleted(deletedCount)` later.

        } catch (Exception e) {
            log.error("Failed to execute scheduled cleanup of expired short links", e);
        }
    }
}
