package com.tlavu.linkforge.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.temporal.ChronoUnit;

@Getter
@RequiredArgsConstructor
public enum VipPackage {
    MONTHLY("VIP_1_MONTH", 50000, 30, ChronoUnit.DAYS),
    QUARTERLY("VIP_3_MONTHS", 135000, 90, ChronoUnit.DAYS), // 10% discount from 150k
    YEARLY("VIP_1_YEAR", 450000, 365, ChronoUnit.DAYS); // 25% discount from 600k

    private final String code;
    private final int priceVnd;
    private final int durationDuration;
    private final ChronoUnit durationUnit;

    public static VipPackage fromCode(String code) {
        for (VipPackage pkg : values()) {
            if (pkg.code.equals(code)) {
                return pkg;
            }
        }
        throw new IllegalArgumentException("Invalid VIP Package code: " + code);
    }
}
