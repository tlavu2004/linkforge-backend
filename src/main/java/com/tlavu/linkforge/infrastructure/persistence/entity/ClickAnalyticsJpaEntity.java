package com.tlavu.linkforge.infrastructure.persistence.entity;

import com.tlavu.linkforge.domain.entity.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "click_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClickAnalyticsJpaEntity {

    @Id
    private Long id;

    @Column(name = "short_code", nullable = false, length = 15)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "country", length = 10)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "device_type", length = 10)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;
}
