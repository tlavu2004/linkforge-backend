package com.tlavu.linkforge.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickAnalytics {
    private Long id;
    private String shortCode;
    private Instant clickedAt;
    private String ipAddress;
    private String userAgent;
    private String country;
    private String city;
    private DeviceType deviceType;
    private String referrer;

    public static ClickAnalytics create(Long id, String shortCode, Instant clickedAt, String ipAddress, 
                                     String userAgent, String country, String city, 
                                     DeviceType deviceType, String referrer) {
        return ClickAnalytics.builder()
                .id(id)
                .shortCode(shortCode)
                .clickedAt(clickedAt != null ? clickedAt : Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .country(country)
                .city(city)
                .deviceType(deviceType != null ? deviceType : DeviceType.UNKNOWN)
                .referrer(referrer)
                .build();
    }
}
