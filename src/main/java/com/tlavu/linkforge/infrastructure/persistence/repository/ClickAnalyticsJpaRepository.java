package com.tlavu.linkforge.infrastructure.persistence.repository;

import com.tlavu.linkforge.infrastructure.persistence.entity.ClickAnalyticsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickAnalyticsJpaRepository extends JpaRepository<ClickAnalyticsJpaEntity, Long> {

    List<ClickAnalyticsJpaEntity> findByShortCodeAndClickedAtBetween(String shortCode, Instant from, Instant to);

    @Query("SELECT c.country, COUNT(c) FROM ClickAnalyticsJpaEntity c WHERE c.shortCode = :shortCode GROUP BY c.country")
    List<Object[]> countByCountry(@Param("shortCode") String shortCode);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickAnalyticsJpaEntity c WHERE c.shortCode = :shortCode GROUP BY c.deviceType")
    List<Object[]> countByDeviceType(@Param("shortCode") String shortCode);

    long countByShortCode(String shortCode);

    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM ClickAnalyticsJpaEntity c WHERE c.shortCode = :shortCode")
    long countUniqueVisitors(@Param("shortCode") String shortCode);
}
