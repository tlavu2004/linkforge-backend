package com.tlavu.linkforge.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@SuppressWarnings("null")
public class TwoLevelCacheService implements ShortLinkCacheService {

    private final Cache<String, ShortLinkResponse> localCache;
    private final ShortLinkCacheServiceImpl redisCache;

    public TwoLevelCacheService(
            Cache<String, ShortLinkResponse> shortLinkLocalCache,
            RedisTemplate<String, Object> redisTemplate) {
        this.localCache = shortLinkLocalCache;
        this.redisCache = new ShortLinkCacheServiceImpl(redisTemplate);
    }

    @Override
    @NonNull
    public Optional<ShortLinkResponse> getShortLink(@NonNull String shortCode) {
        // 1. Check L1 (Caffeine)
        ShortLinkResponse cached = localCache.getIfPresent(shortCode);
        if (cached != null) {
            log.debug("L1 Cache HIT for shortCode: {}", shortCode);
            return Optional.of(cached);
        }

        // 2. Check L2 (Redis)
        Optional<ShortLinkResponse> redisResult = redisCache.getShortLink(shortCode);
        if (redisResult.isPresent()) {
            log.debug("L2 Cache HIT for shortCode: {}, promoting to L1", shortCode);
            localCache.put(shortCode, redisResult.get());
            return redisResult;
        }

        log.debug("L1+L2 Cache MISS for shortCode: {}", shortCode);
        return Optional.empty();
    }

    @Override
    public void saveShortLink(@NonNull String shortCode, @NonNull ShortLinkResponse response) {
        // Save to both layers
        redisCache.saveShortLink(shortCode, response);
        localCache.put(shortCode, response);
        log.debug("Saved to L1+L2 cache for shortCode: {}", shortCode);
    }

    @Override
    public void evictShortLink(@NonNull String shortCode) {
        // Evict from both layers
        localCache.invalidate(shortCode);
        redisCache.evictShortLink(shortCode);
        log.debug("Evicted from L1+L2 cache for shortCode: {}", shortCode);
    }
}
