package com.example.nidar.sos.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidConfig.Priority;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FcmService {

    // ── High-priority notification — wakes screen, shown in notification tray ─
    public void sendHighPriority(String fcmToken, String title,
                                 String body, Map<String, String> data) {
        Message message = Message.builder()
            .setToken(fcmToken)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .putAllData(data)
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(Priority.HIGH)
                .build())
            .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("🔔 FCM HIGH sent messageId={} token={}", messageId, maskToken(fcmToken));
        } catch (FirebaseMessagingException e) {
            log.warn("🔔 FCM HIGH failed token={} error={}", maskToken(fcmToken), e.getMessage());
        }
    }

    // ── Silent data-only message — no notification shown; app handles it ──────
    public void sendSilentData(String fcmToken, Map<String, String> data) {
        Message message = Message.builder()
            .setToken(fcmToken)
            .putAllData(data)
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(Priority.HIGH)   // high priority so Doze doesn't delay it
                .build())
            .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("🔕 FCM SILENT sent messageId={} token={}", messageId, maskToken(fcmToken));
        } catch (FirebaseMessagingException e) {
            log.warn("🔕 FCM SILENT failed token={} error={}", maskToken(fcmToken), e.getMessage());
        }
    }

    // Mask token for safe logging — show first 8 chars only
    private String maskToken(String token) {
        return token != null && token.length() > 8
            ? token.substring(0, 8) + "..."
            : "null";
    }
}