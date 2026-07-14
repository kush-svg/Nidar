package com.example.nidar.common.messaging;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessagingService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Gson gson = new Gson();

    public void publishSosAlert(String sessionId, String userId, double lat, double lng) {
        Map<String, String> message = Map.of(
            "type",      "SOS_ALERT",
            "sessionId", sessionId,
            "userId",    userId,
            "lat",       String.valueOf(lat),
            "lng",       String.valueOf(lng)
        );

        kafkaTemplate.send("sos-alerts", sessionId, gson.toJson(message))
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("SOS alert publish failed: {}", ex.getMessage());
                } else {
                    log.info("SOS alert published: {}", sessionId);
                }
            });
    }

    public void publishLocationIncident(String h3Index, String incidentType, double lat, double lng) {
        Map<String, String> message = Map.of(
            "type",         "LOCATION_INCIDENT",
            "h3Index",      h3Index,
            "incidentType", incidentType,
            "lat",          String.valueOf(lat),
            "lng",          String.valueOf(lng)
        );

        kafkaTemplate.send("location-incidents", h3Index, gson.toJson(message))
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Incident publish failed: {}", ex.getMessage());
                } else {
                    log.debug("Incident published to h3: {}", h3Index);
                }
            });
    }
}
