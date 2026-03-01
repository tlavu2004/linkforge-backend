package com.tlavu.linkforge.infrastructure.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    /**
     * Generate a 6-digit OTP, store in Redis with key otp:{purpose}:{email}
     */
    public String generateAndStore(String email, String purpose) {
        String otp = generateOtp();
        String key = buildKey(email, purpose);
        redisTemplate.opsForValue().set(key, otp, OTP_TTL);
        return otp;
    }

    /**
     * Verify OTP and delete from Redis if valid
     */
    public boolean verify(String email, String purpose, String otp) {
        String key = buildKey(email, purpose);
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    private String buildKey(String email, String purpose) {
        return "otp:" + purpose + ":" + email.toLowerCase();
    }
}
