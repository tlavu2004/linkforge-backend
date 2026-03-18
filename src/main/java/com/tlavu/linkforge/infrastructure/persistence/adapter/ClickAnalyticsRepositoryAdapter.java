package com.tlavu.linkforge.infrastructure.persistence.adapter;

import com.tlavu.linkforge.domain.entity.ClickAnalytics;
import com.tlavu.linkforge.domain.entity.DeviceType;
import com.tlavu.linkforge.domain.repository.ClickAnalyticsRepository;
import com.tlavu.linkforge.infrastructure.persistence.entity.ClickAnalyticsJpaEntity;
import com.tlavu.linkforge.infrastructure.persistence.mapper.ClickAnalyticsMapper;
import com.tlavu.linkforge.infrastructure.persistence.repository.ClickAnalyticsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ClickAnalyticsRepositoryAdapter implements ClickAnalyticsRepository {

    private final ClickAnalyticsJpaRepository jpaRepository;
    private final ClickAnalyticsMapper mapper;

    @Override
    public void save(@NonNull ClickAnalytics analytics) {
        ClickAnalyticsJpaEntity jpaEntity = mapper.toJpaEntity(analytics);
        jpaRepository.save(jpaEntity);
    }

    @Override
    public List<ClickAnalytics> findByShortCode(String shortCode, Instant from, Instant to) {
        return jpaRepository.findByShortCodeAndClickedAtBetween(shortCode, from, to)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> countByCountry(String shortCode, Instant from, Instant to) {
        List<Object[]> results = jpaRepository.countByCountry(shortCode, from, to);
        Map<String, Long> map = new HashMap<>();
        for (Object[] result : results) {
            map.put((String) result[0], (Long) result[1]);
        }
        return map;
    }


    @Override
    public Map<DeviceType, Long> countByDeviceType(String shortCode, Instant from, Instant to) {
        List<Object[]> results = jpaRepository.countByDeviceType(shortCode, from, to);
        Map<DeviceType, Long> map = new HashMap<>();
        for (Object[] result : results) {
            map.put((DeviceType) result[0], (Long) result[1]);
        }
        return map;
    }


    @Override
    public Map<String, Long> countByReferrer(String shortCode, Instant from, Instant to) {
        List<Object[]> results = jpaRepository.countByReferrer(shortCode, from, to);
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] result : results) {
            map.put((String) result[0] != null ? (String) result[0] : "Direct", (Long) result[1]);
        }
        return map;
    }


    @Override
    public Map<LocalDate, Long> getDailyClickStats(String shortCode, Instant from, Instant to) {
        List<Object[]> results = jpaRepository.getDailyStats(shortCode, from, to);
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (Object[] result : results) {
            // result[0] is java.sql.Date from native query, convert to LocalDate
            LocalDate date = ((Date) result[0]).toLocalDate();
            map.put(date, (Long) result[1]);
        }
        return map;
    }

    @Override
    public long countTotalClicks(String shortCode, Instant from, Instant to) {
        return jpaRepository.countTotalClicks(shortCode, from, to);
    }

    @Override
    public long countUniqueVisitors(String shortCode, Instant from, Instant to) {
        return jpaRepository.countUniqueVisitors(shortCode, from, to);
    }

}
