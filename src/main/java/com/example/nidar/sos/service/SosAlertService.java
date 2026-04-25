package com.example.nidar.sos.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.TrustedContact;
import com.example.nidar.auth.repository.TrustedContactRepository;
import com.example.nidar.auth.repository.UserRepository;

import com.example.nidar.common.util.H3SnapUtil;

@Service
@RequiredArgsConstructor
public class SosAlertService {

    private final TrustedContactRepository contactRepository;
    private final UserRepository           userRepository;
    private final FcmService               fcmService;
    private final SmsService               smsService;
    private final H3SnapUtil               h3SnapUtil;

    // ── Step B: Tier 1 — Trusted contacts ────────────────────────────────────
    @Async("sosTaskExecutor")   // named thread pool — see config below
    public void alertTrustedContacts(String userId, String sessionId,
                                     double lat, double lng) {
        List<TrustedContact> contacts = contactRepository.findByUserId(userId);
        if (contacts.isEmpty()) return;

        String trackingUrl = "https://nidar.app/sos/" + sessionId;

        // Get the user's name for the SMS message
        String userName = userRepository.findNameById(userId);

        for (TrustedContact contact : contacts) {

            // Action 1: FCM — wakes phone even in Doze mode
            if (contact.getFcmToken() != null) {
                fcmService.sendHighPriority(
                    contact.getFcmToken(),
                    "SOS ALERT",
                    userName + " has triggered an emergency SOS!",
                    Map.of(
                        "type",       "SOS_ALERT",
                        "sessionId",  sessionId,
                        "trackingUrl", trackingUrl,
                        "lat",        String.valueOf(lat),
                        "lng",        String.valueOf(lng)
                    )
                );
            }

            // Action 2: SMS — fallback if FCM fails or phone is off
            if (contact.getPhoneNumber() != null) {
                smsService.send(
                    contact.getPhoneNumber(),
                    "EMERGENCY: " + userName + " has triggered an SOS. " +
                    "Live tracking: " + trackingUrl
                );
            }
        }
    }

    // ── Step C: Tier 2 — Nearby protectors ───────────────────────────────────
    @Async("sosTaskExecutor")
    public void alertNearbyProtectors(String h3Index, String sessionId,
                                      double lat, double lng) {

        // Get the H3 cell + its ring-1 neighbors (~2km coverage at resolution 7)
        // gridDisk returns unmodifiable list — wrap so we can add the origin cell
        List<String> searchCells = new ArrayList<>(h3SnapUtil.getNeighborCells(h3Index, 1));
        searchCells.add(h3Index);   // include the origin cell

        // Find active protectors in those cells
        long cutoff = System.currentTimeMillis() / 1000 - (30 * 60);  // active in last 30 mins
        List<User> protectors = userRepository.findActiveProtectorsInCells(searchCells, cutoff);
        if (protectors.isEmpty()) return;

        for (User protector : protectors) {
            if (protector.getFcmToken() == null) continue;

            // Silent data payload — no notification shown by FCM directly.
            // The Android app receives this, calculates exact distance,
            // and decides whether to show the overlay.
            fcmService.sendSilentData(
                protector.getFcmToken(),
                Map.of(
                    "type",      "SOS_NEARBY",
                    "sessionId", sessionId,
                    "lat",       String.valueOf(lat),
                    "lng",       String.valueOf(lng)
                )
            );
        }
    }
}
