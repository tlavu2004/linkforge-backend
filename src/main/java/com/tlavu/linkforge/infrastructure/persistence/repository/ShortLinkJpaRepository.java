package com.tlavu.linkforge.infrastructure.persistence.repository;

import com.tlavu.linkforge.infrastructure.persistence.entity.ShortLinkJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ShortLinkJpaRepository extends JpaRepository<ShortLinkJpaEntity, Long> {
    Optional<ShortLinkJpaEntity> findByShortCode(String shortCode);

    Page<ShortLinkJpaEntity> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT s FROM ShortLinkJpaEntity s WHERE s.userId = :userId AND " +
            "(LOWER(s.originalUrl) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.shortCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ShortLinkJpaEntity> findByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword,
            Pageable pageable);

    @Modifying
    @Query("UPDATE ShortLinkJpaEntity s SET s.clickCount = s.clickCount + 1 WHERE s.shortCode = :shortCode")
    void incrementClickCountByShortCode(@Param("shortCode") String shortCode);

    @Modifying
    @Query("DELETE FROM ShortLinkJpaEntity s WHERE s.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
