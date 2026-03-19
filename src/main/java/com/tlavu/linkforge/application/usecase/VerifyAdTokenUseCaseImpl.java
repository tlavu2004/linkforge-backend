package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.exception.AdTokenVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyAdTokenUseCaseImpl implements VerifyAdTokenUseCase {

    private final StringRedisTemplate redisTemplate;
    private final ResolveShortLinkUseCase resolveShortLinkUseCase;

    private static final String AD_TOKEN_PREFIX = "ad_token:";
    private static final long WAIT_TIME_MS = 5000;

    @Override
    public String execute(String token, String shortCode, String ipAddress, String userAgent, String referrer) {
        log.info("Verifying ad token for shortCode: {}, IP: {}", shortCode, ipAddress);
        String key = AD_TOKEN_PREFIX + token;
        String tokenData = redisTemplate.opsForValue().get(key);

        if (tokenData == null) {
            log.warn("Ad token validation failed: token not found for key {}", key);
            throw new AdTokenVerificationException("ad.token_invalid");
        }

        String[] parts = tokenData.split(":");
        if (parts.length != 2) {
            throw new AdTokenVerificationException("ad.token_malformed");
        }

        String storedShortCode = parts[0];
        long createdAt = Long.parseLong(parts[1]);

        if (!storedShortCode.equals(shortCode)) {
            throw new AdTokenVerificationException("ad.token_mismatch");
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - createdAt < WAIT_TIME_MS) {
            throw new AdTokenVerificationException("ad.token_wait");
        }

        // Token is valid and time has passed
        redisTemplate.delete(key);

        // Fetch and return the original URL, and track the click since the user waited 5s
        log.info("Ad token valid, proceeding to resolve and track click for shortCode: {}", shortCode);
        ShortLinkResponse shortLinkResponse = resolveShortLinkUseCase.execute(shortCode, true, ipAddress, userAgent, referrer);
        return shortLinkResponse.originalUrl();
    }
}
