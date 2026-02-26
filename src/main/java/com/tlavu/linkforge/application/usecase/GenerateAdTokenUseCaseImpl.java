package com.tlavu.linkforge.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GenerateAdTokenUseCaseImpl implements GenerateAdTokenUseCase {

    private final StringRedisTemplate redisTemplate;
    private static final String AD_TOKEN_PREFIX = "ad_token:";
    private static final long EXPIRE_MINUTES = 15;

    @Override
    public String execute(String shortCode) {
        String adToken = UUID.randomUUID().toString();
        String key = AD_TOKEN_PREFIX + adToken;

        // Value format: "{shortCode}:{currentTimeMillis}"
        String value = shortCode + ":" + System.currentTimeMillis();

        redisTemplate.opsForValue().set(key, value, EXPIRE_MINUTES, TimeUnit.MINUTES);

        return adToken;
    }
}
