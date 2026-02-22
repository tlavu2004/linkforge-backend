package com.tlavu.linkforge.infrastructure.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    void doFilterInternal_generatesNewIdIfNotPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Assert MDC is populated DURING the filter chain execution
        FilterChain filterChain = (req, res) -> {
            String mdcId = MDC.get("correlationId");
            assertNotNull(mdcId);
            assertFalse(mdcId.isEmpty());

            // Check response header
            assertEquals(mdcId, response.getHeader("X-Correlation-ID"));
        };

        filter.doFilter(request, response, filterChain);

        // Assert MDC is cleared AFTER execution
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void doFilterInternal_usesExistingIdIfPresent() throws Exception {
        String existingId = "req-12345";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            assertEquals(existingId, MDC.get("correlationId"));
            assertEquals(existingId, response.getHeader("X-Correlation-ID"));
        };

        filter.doFilter(request, response, filterChain);

        assertNull(MDC.get("correlationId"));
    }
}
