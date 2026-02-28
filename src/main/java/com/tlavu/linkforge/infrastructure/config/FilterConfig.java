package com.tlavu.linkforge.infrastructure.config;

import com.tlavu.linkforge.infrastructure.ratelimit.RateLimitFilter;
import com.tlavu.linkforge.application.port.in.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import com.tlavu.linkforge.infrastructure.logging.CorrelationIdFilter;

@Configuration
public class FilterConfig {

    private final RateLimiter rateLimiter;

    // Default: 60 requests per 1 minute (60 seconds)
    @Value("${rate-limit.max-requests:60}")
    private int maxRequests;

    @Value("${rate-limit.time-window-seconds:60}")
    private int timeWindowSeconds;

    public FilterConfig(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistrationBean() {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();

        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimiter, maxRequests, timeWindowSeconds);
        registrationBean.setFilter(rateLimitFilter);

        // Define which URLs to protect.
        // We protect link creation and redirects.
        registrationBean.addUrlPatterns("/api/v1/links/*");
        registrationBean.addUrlPatterns("/r/*");

        // Give it an order so it runs early in the filter chain
        registrationBean.setOrder(1);

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistrationBean() {
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new CorrelationIdFilter());
        registrationBean.addUrlPatterns("/api/*", "/r/*", "/actuator/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registrationBean;
    }
}
