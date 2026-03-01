package com.tlavu.linkforge.domain.repository;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface ShortLinkRepository {

    ShortLink save(ShortLink shortLink);

    Optional<ShortLink> findByShortCode(ShortCode shortCode);

    Optional<ShortLink> findById(Long id);

    void delete(Long id);

    void incrementClickCount(ShortCode shortCode);

    int deleteExpiredLinks(Instant now);

    Page<ShortLink> findByUserId(Long userId, Pageable pageable);
}
