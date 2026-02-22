package com.tlavu.linkforge.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter linksCreatedCounter;
    private final Counter linksResolvedTotalCounter;
    private final Counter linksResolvedCacheHitsCounter;
    private final Counter linksResolvedCacheMissesCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.linksCreatedCounter = Counter.builder("shortlinks.created.total")
                .description("Total number of short links created")
                .register(meterRegistry);

        this.linksResolvedTotalCounter = Counter.builder("shortlinks.resolved.total")
                .description("Total number of short link resolution requests")
                .register(meterRegistry);

        this.linksResolvedCacheHitsCounter = Counter.builder("shortlinks.resolved.cache.hits")
                .description("Total number of short link resolutions served from cache")
                .register(meterRegistry);

        this.linksResolvedCacheMissesCounter = Counter.builder("shortlinks.resolved.cache.misses")
                .description("Total number of short link resolutions requiring database access")
                .register(meterRegistry);
    }

    public void incrementLinksCreated() {
        linksCreatedCounter.increment();
    }

    public void incrementLinksResolved() {
        linksResolvedTotalCounter.increment();
    }

    public void incrementCacheHits() {
        linksResolvedCacheHitsCounter.increment();
    }

    public void incrementCacheMisses() {
        linksResolvedCacheMissesCounter.increment();
    }
}
