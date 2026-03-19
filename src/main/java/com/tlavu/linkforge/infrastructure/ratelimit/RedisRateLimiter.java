package com.tlavu.linkforge.infrastructure.ratelimit;

import com.tlavu.linkforge.application.port.in.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
@SuppressWarnings("null")
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> redisScript;

    public RedisRateLimiter(StringRedisTemplate redisTemplate, RedisScript<Long> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.redisScript = rateLimitScript;
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, int timeWindowSeconds) {
        try {
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList("rate_limit:" + key),
                    String.valueOf(maxRequests),
                    String.valueOf(timeWindowSeconds));

            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.error("Failed to execute rate limit for key: {}. Error: {}", key, e.getMessage());
            // Fail open: Default to true to not block legitimate traffic on Redis failure
            return true;
        }
    }
}
