package com.tlavu.linkforge.infrastructure.service;

import com.tlavu.linkforge.domain.entity.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAgentParserTest {

    private final UserAgentParser parser = new UserAgentParser();

    @Test
    void shouldParseMobile() {
        String iphone = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Mobile/15E148 Safari/604.1";
        String android = "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.162 Mobile Safari/537.36";
        
        assertEquals(DeviceType.MOBILE, parser.parseDeviceType(iphone));
        assertEquals(DeviceType.MOBILE, parser.parseDeviceType(android));
    }

    @Test
    void shouldParseTablet() {
        String ipad = "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1 Mobile/15E148 Safari/604.1";
        
        assertEquals(DeviceType.TABLET, parser.parseDeviceType(ipad));
    }

    @Test
    void shouldParseDesktop() {
        String chrome = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        String safari = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15";
        
        assertEquals(DeviceType.DESKTOP, parser.parseDeviceType(chrome));
        assertEquals(DeviceType.DESKTOP, parser.parseDeviceType(safari));
    }

    @Test
    void shouldReturnUnknownForEmpty() {
        assertEquals(DeviceType.UNKNOWN, parser.parseDeviceType(null));
        assertEquals(DeviceType.UNKNOWN, parser.parseDeviceType(""));
        assertEquals(DeviceType.UNKNOWN, parser.parseDeviceType("Some random bot/1.0"));
    }
}
