package com.example.nidar.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.example.nidar.sos.service.SmsService;
import java.security.SecureRandom;
import java.time.Duration;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SmsService                    smsService;

    private static final int    OTP_LENGTH          = 6;
    private static final long   OTP_TTL_MINUTES     = 5;
    private static final int    MAX_OTP_REQUESTS    = 3;
    private static final long   RATE_LIMIT_MINUTES  = 10;

    private static final String OTP_KEY_PREFIX      = "otp:";
    private static final String RATE_LIMIT_PREFIX   = "otp:rate:";

    public void generateAndSend(String phoneNumber) {

        // ── Rate limit — max 3 OTPs per phone per 10 minutes ─────────────────
        String rateKey   = RATE_LIMIT_PREFIX + phoneNumber;
        String countStr  = redisTemplate.opsForValue().get(rateKey);
        int    count     = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= MAX_OTP_REQUESTS) {
            throw new RuntimeException("Too many OTP requests. Try again in 10 minutes.");
        }

        // Increment rate counter
        redisTemplate.opsForValue().increment(rateKey);
        redisTemplate.expire(rateKey, Duration.ofMinutes(RATE_LIMIT_MINUTES));

        // ── Generate 6-digit OTP ──────────────────────────────────────────────
        String otp = String.format("%0" + OTP_LENGTH + "d", new SecureRandom().nextInt(999999));

        // ── Store in Redis with 5-min TTL ─────────────────────────────────────
        String otpKey = OTP_KEY_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofMinutes(OTP_TTL_MINUTES));

        // ── Send via MSG91 ────────────────────────────────────────────────────
        smsService.send(phoneNumber, "Your Nidar OTP is: " + otp +
                                     ". Valid for 5 minutes. Do not share.");
        log.info("📩 OTP for {} is: {}", phoneNumber, otp);
    }

    public boolean verify(String phoneNumber, String otp) {
        String otpKey = OTP_KEY_PREFIX + phoneNumber;
        String stored = redisTemplate.opsForValue().get(otpKey);

        log.info("🔍 Stored OTP: {}", stored);
        log.info("🔍 Received OTP: {}", otp);

        if (stored == null) {
            throw new RuntimeException("OTP expired or not found");
        }

        if (!stored.equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        redisTemplate.delete(otpKey);
        return true;
    }
}
