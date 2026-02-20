package com.tlavu.linkforge.application.port.in;

/**
 * Port for rate limiting functionality to protect the application from abuse.
 */
public interface RateLimiter {

    /**
     * Checks if the given key (e.g., IP address) is allowed to perform an action
     * based on rate limits.
     *
     * @param key               The unique identifier to rate limit on (e.g.,
     *                          "ip:192.168.1.1").
     * @param maxRequests       The maximum number of requests allowed in the time
     *                          window.
     * @param timeWindowSeconds The time window in seconds.
     * @return true if the request is allowed, false if the rate limit is exceeded.
     */
    boolean isAllowed(String key, int maxRequests, int timeWindowSeconds);
}
