package com.tlavu.linkforge.domain.repository;

import com.tlavu.linkforge.domain.entity.ClickAnalytics;
import com.tlavu.linkforge.domain.entity.DeviceType;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ClickAnalyticsRepository {
    void save(@NonNull ClickAnalytics analytics);

    List<ClickAnalytics> findByShortCode(String shortCode, Instant from, Instant to);

    Map<String, Long> countByCountry(String shortCode);

    Map<DeviceType, Long> countByDeviceType(@NonNull String shortCode);

    Map<String, Long> countByReferrer(@NonNull String shortCode);

    Map<java.time.LocalDate, Long> getDailyClickStats(@NonNull String shortCode, @NonNull Instant from, @NonNull Instant to);

    long countTotalClicks(@NonNull String shortCode);
    
    long countUniqueVisitors(String shortCode);
}
