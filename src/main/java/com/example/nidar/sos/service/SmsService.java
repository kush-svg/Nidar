package com.example.nidar.sos.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate;

    // ✅ Default values added → prevents app crash
    @Value("${msg91.auth-key:dummy_key}")
    private String authKey;

    @Value("${msg91.sender-id:sender_id}")
    private String senderId;

    @Value("${msg91.template-id:123456}")
    private String templateId;

    public void send(String phoneNumber, String message) {

        // 🔥 DEV MODE (recommended)
        if (authKey.equals("dummy_key")) {
            log.info("📩 [MOCK SMS] to {}: {}", phoneNumber, message);
            return;
        }

        // MSG91 API URL
        String url = "https://api.msg91.com/api/v5/flow/";

        Map<String, Object> payload = Map.of(
            "template_id", templateId,
            "short_url", "0",
            "recipients", List.of(Map.of(
                "mobiles", phoneNumber,   // format: 91XXXXXXXXXX
                "message", message
            ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("authkey", authKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(
                url,
                new HttpEntity<>(payload, headers),
                String.class
            );

            log.info("✅ SMS sent to {}", phoneNumber);

        } catch (RuntimeException e) {
            log.error("❌ SMS send failed to {}: {}", phoneNumber, e.getMessage());
        }
    }
}