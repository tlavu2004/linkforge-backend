package com.tlavu.linkforge.infrastructure.ratelimit;

import com.tlavu.linkforge.application.port.in.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
@SuppressWarnings("null")
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        this.redisScript.setResultType(Long.class);
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, int timeWindowSeconds) {
        try {
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList("rate_limit:" + key),
                    String.valueOf(maxRequests),
                    String.valueOf(timeWindowSeconds));

            if (result == null) {
                log.warn("Rate limit Lua script returned null for key: {}. Allowing request by default.", key);
                return true;
            }

            return result == 1L;
        } catch (Exception e) {
            log.error("Failed to execute rate limit for key: {}. Error: {}", key, e.getMessage());
            // Fail open: Default to true to not block legitimate traffic on Redis failure
            return true;
        }
    }
}
