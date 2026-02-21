package com.tlavu.linkforge.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = extractOrGenerateCorrelationId(request);

        // Put the correlation ID into the MDC so that logger can access it
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, correlationId);

        try {
            // Also add it to the response header so clients can trace requests
            response.addHeader(CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            // Important: ALWAYS clear the MDC to prevent memory leaks and incorrect logs
            // across thread pools
            MDC.remove(CORRELATION_ID_LOG_VAR_NAME);
        }
    }

    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String headerId = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(headerId)) {
            return headerId;
        }
        return UUID.randomUUID().toString();
    }
}
