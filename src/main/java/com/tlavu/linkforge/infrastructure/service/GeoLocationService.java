package com.tlavu.linkforge.infrastructure.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@SuppressWarnings("null")
public class GeoLocationService {

    private final RestTemplate restTemplate;
    private static final String IP_API_URL = "http://ip-api.com/json/%s?fields=status,message,countryCode,city";

    public GeoLocationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GeoData getLocation(@NonNull String ip) {
        if (ip == null || ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return new GeoData("Local", "Local");
        }

        try {
            String url = String.format(IP_API_URL, ip);
            IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);

            if (response != null && "success".equalsIgnoreCase(response.getStatus())) {
                return new GeoData(response.getCountryCode(), response.getCity());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch geo location for IP {}: {}", ip, e.getMessage());
        }

        return new GeoData("Unknown", "Unknown");
    }

    @Data
    public static class GeoData {
        private final String countryCode;
        private final String city;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IpApiResponse {
        private String status;
        private String countryCode;
        private String city;
    }
}
