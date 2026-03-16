package com.tlavu.linkforge.application.dto.response;

import com.tlavu.linkforge.domain.entity.DeviceType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Map;

@Builder
public record LinkStatsResponse(
    String shortCode,
    long totalClicks,
    long uniqueVisitors,
    Map<String, Long> clicksByCountry,
    Map<DeviceType, Long> clicksByDeviceType,
    Map<String, Long> clicksByReferrer,
    Map<LocalDate, Long> dailyStats
) {}
