package com.tlavu.linkforge.domain.repository;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.valueobject.ShortCode;

import java.util.Optional;

public interface ShortLinkRepository {

    ShortLink save(ShortLink shortLink);

    Optional<ShortLink> findByShortCode(ShortCode shortCode);

    Optional<ShortLink> findById(Long id);

    void delete(Long id);

    void incrementClickCount(ShortCode shortCode);

    int deleteExpiredLinks(java.time.Instant now);
}
