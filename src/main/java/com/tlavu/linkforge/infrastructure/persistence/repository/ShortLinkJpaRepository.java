package com.tlavu.linkforge.infrastructure.persistence.repository;

import com.tlavu.linkforge.infrastructure.persistence.entity.ShortLinkJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortLinkJpaRepository extends JpaRepository<ShortLinkJpaEntity, Long> {
    Optional<ShortLinkJpaEntity> findByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE ShortLinkJpaEntity s SET s.clickCount = s.clickCount + 1 WHERE s.shortCode = :shortCode")
    void incrementClickCountByShortCode(@Param("shortCode") String shortCode);

    @Modifying
    @Query("DELETE FROM ShortLinkJpaEntity s WHERE s.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") java.time.Instant now);
}
