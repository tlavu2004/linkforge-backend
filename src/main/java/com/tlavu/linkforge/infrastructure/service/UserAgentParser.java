package com.tlavu.linkforge.infrastructure.service;

import com.tlavu.linkforge.domain.entity.DeviceType;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class UserAgentParser {

    private static final Pattern MOBILE_PATTERN = Pattern.compile(
            "Android|webOS|iPhone|iPod|BlackBerry|IEMobile|Opera Mini", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLET_PATTERN = Pattern.compile(
            "Tablet|iPad|PlayBook", Pattern.CASE_INSENSITIVE);

    /**
     * Parses User-Agent string to determine device type.
     * 
     * @param userAgent the UA string
     * @return DeviceType enum
     */
    public DeviceType parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceType.UNKNOWN;
        }

        if (TABLET_PATTERN.matcher(userAgent).find()) {
            return DeviceType.TABLET;
        }

        if (MOBILE_PATTERN.matcher(userAgent).find()) {
            return DeviceType.MOBILE;
        }

        // Default to DESKTOP for most common non-mobile browsers
        if (userAgent.contains("Mozilla/") || userAgent.contains("Chrome/") || userAgent.contains("Safari/")) {
            return DeviceType.DESKTOP;
        }

        return DeviceType.UNKNOWN;
    }
}
