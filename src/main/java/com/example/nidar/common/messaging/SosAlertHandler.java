package com.example.nidar.common.messaging;

import com.example.nidar.sos.service.SosAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SosAlertHandler {

    private final SosAlertService sosAlertService;

    public void handle(Map<String, String> data) {
        String sessionId = data.get("sessionId");
        String userId    = data.get("userId");
        double lat       = Double.parseDouble(data.getOrDefault("lat", "0.0"));
        double lng       = Double.parseDouble(data.getOrDefault("lng", "0.0"));

        if (sessionId == null || userId == null) {
            log.warn("Invalid SOS alert payload — missing sessionId or userId");
            return;
        }

        log.info("Processing SOS alert for session: {}", sessionId);

        // Fan out to trusted contacts + nearby protectors
        sosAlertService.alertTrustedContacts(userId, sessionId, lat, lng);
        sosAlertService.alertNearbyProtectors(
            data.getOrDefault("h3Index", ""), sessionId, lat, lng
        );
    }
}