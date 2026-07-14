package com.example.nidar.common.messaging;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final SosAlertHandler sosAlertHandler;
    private final IncidentAlertHandler incidentAlertHandler;
    private final Gson gson = new Gson();

    @KafkaListener(topics = "sos-alerts", groupId = "nidar-sos-group")
    public void handleSosAlert(String payload) {
        try {
            log.info("Received SOS alert: {}", payload);
            Map<String, String> data = gson.fromJson(
                payload,
                new TypeToken<Map<String, String>>(){}.getType()
            );
            sosAlertHandler.handle(data);
        } catch (Exception e) {
            log.error("SOS alert handling failed: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "location-incidents", groupId = "nidar-incident-group")
    public void handleLocationIncident(String payload) {
        try {
            log.debug("Received location incident: {}", payload);
            Map<String, String> data = gson.fromJson(
                payload,
                new TypeToken<Map<String, String>>(){}.getType()
            );
            incidentAlertHandler.handle(data);
        } catch (Exception e) {
            log.error("Location incident handling failed: {}", e.getMessage(), e);
        }
    }
}
