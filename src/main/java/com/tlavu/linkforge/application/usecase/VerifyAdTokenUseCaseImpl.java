package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.exception.AdTokenVerificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerifyAdTokenUseCaseImpl implements VerifyAdTokenUseCase {

    private final StringRedisTemplate redisTemplate;
    private final ResolveShortLinkUseCase resolveShortLinkUseCase;

    private static final String AD_TOKEN_PREFIX = "ad_token:";
    private static final long WAIT_TIME_MS = 5000;

    @Override
    public String execute(String token, String shortCode) {
        String key = AD_TOKEN_PREFIX + token;
        String tokenData = redisTemplate.opsForValue().get(key);

        if (tokenData == null) {
            throw new AdTokenVerificationException("Invalid or expired ad token");
        }

        String[] parts = tokenData.split(":");
        if (parts.length != 2) {
            throw new AdTokenVerificationException("Malformed ad token data");
        }

        String storedShortCode = parts[0];
        long createdAt = Long.parseLong(parts[1]);

        if (!storedShortCode.equals(shortCode)) {
            throw new AdTokenVerificationException("Ad token does not match short code");
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - createdAt < WAIT_TIME_MS) {
            throw new AdTokenVerificationException("You must wait 5 seconds before verifying");
        }

        // Token is valid and time has passed
        redisTemplate.delete(key);

        // Fetch and return the original URL, and track the click since the user waited
        // 5s
        ShortLinkResponse shortLinkResponse = resolveShortLinkUseCase.execute(shortCode, true);
        return shortLinkResponse.originalUrl();
    }
}
