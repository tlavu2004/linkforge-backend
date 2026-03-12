package com.tlavu.linkforge.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CaffeineConfig {

    /**
     * L1 in-memory cache for short link redirect responses.
     * - Max 10,000 entries to cap memory usage
     * - TTL 60s to keep data fresh (short-lived to avoid stale redirects)
     * - recordStats() enables Micrometer/Prometheus metrics
     */
    @Bean
    public Cache<String, ShortLinkResponse> shortLinkLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(60))
                .recordStats()
                .build();
    }
}
