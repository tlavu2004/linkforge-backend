package com.tlavu.linkforge.infrastructure.cache;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface ShortLinkCacheService {

    /**
     * Retrieve ShortLinkResponse from cache by short code.
     * 
     * @param shortCode the short code
     * @return Optional containing ShortLinkResponse if found, else empty.
     */
    @NonNull
    Optional<ShortLinkResponse> getShortLink(@NonNull String shortCode);

    /**
     * Cache the ShortLinkResponse.
     * 
     * @param shortCode the short code
     * @param response  the response object to cache
     */
    void saveShortLink(@NonNull String shortCode, @NonNull ShortLinkResponse response);

    /**
     * Evict the ShortLinkResponse from cache.
     * 
     * @param shortCode the short code
     */
    void evictShortLink(@NonNull String shortCode);
}
