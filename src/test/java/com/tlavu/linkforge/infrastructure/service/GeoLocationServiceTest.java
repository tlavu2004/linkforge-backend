package com.tlavu.linkforge.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class GeoLocationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeoLocationService geoLocationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        geoLocationService = new GeoLocationService(restTemplate);
    }

    @Test
    void shouldReturnLocalForLocalIp() {
        GeoLocationService.GeoData result = geoLocationService.getLocation("127.0.0.1");
        assertEquals("Local", result.getCountryCode());
        assertEquals("Local", result.getCity());
    }

    @Test
    void shouldReturnUnknownOnException() {
        when(restTemplate.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("API Down"));

        GeoLocationService.GeoData result = geoLocationService.getLocation("8.8.8.8");
        assertEquals("Unknown", result.getCountryCode());
        assertEquals("Unknown", result.getCity());
    }
}
