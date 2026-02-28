package com.tlavu.linkforge.infrastructure.cache;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ShortLinkCacheServiceImpl implements ShortLinkCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "short_link:";
    // Cache duration: 24 hours (can be configurable)
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @Override
    public Optional<ShortLinkResponse> getShortLink(String shortCode) {
        String key = CACHE_PREFIX + shortCode;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ShortLinkResponse response) {
                log.debug("Cache HIT for shortCode: {}", shortCode);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.error("Error retrieving from cache for shortCode: {}", shortCode, e);
        }
        log.debug("Cache MISS for shortCode: {}", shortCode);
        return Optional.empty();
    }

    @Override
    public void saveShortLink(String shortCode, ShortLinkResponse response) {
        String key = CACHE_PREFIX + shortCode;
        try {
            redisTemplate.opsForValue().set(key, response, CACHE_TTL);
            log.debug("Cached shortCode: {}", shortCode);
        } catch (Exception e) {
            log.error("Error saving to cache for shortCode: {}", shortCode, e);
        }
    }

    @Override
    public void evictShortLink(String shortCode) {
        String key = CACHE_PREFIX + shortCode;
        try {
            redisTemplate.delete(key);
            log.debug("Evicted shortCode: {}", shortCode);
        } catch (Exception e) {
            log.error("Error evicting from cache for shortCode: {}", shortCode, e);
        }
    }
}
