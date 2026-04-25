package com.example.nidar.sos.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.nidar.sos.model.SosSession;
import com.example.nidar.sos.model.SosStatus;
import com.example.nidar.sos.repository.SosSessionRepository;
import com.example.nidar.sos.dto.LocationUpdateRequest;
import com.example.nidar.sos.dto.SosTriggerRequest;
import com.example.nidar.sos.dto.SosTriggerResponse;
import com.example.nidar.common.util.H3SnapUtil;

@Service
@RequiredArgsConstructor
public class SosService {

    private final SosSessionRepository sessionRepository;
    private final SosAlertService alertService;   // handles Steps B & C
    private final H3SnapUtil h3SnapUtil;

    @Transactional
    public SosTriggerResponse triggerSos(SosTriggerRequest request) {

        // ── Step A: Create and persist the session synchronously ─────────────
        String sessionId = UUID.randomUUID().toString();
        
        double lat = request.latitude();
        double lng = request.longitude();
        
        String h3Index = h3SnapUtil.cellIndex(lat, lng);

        SosSession session = new SosSession();
        session.setSessionId(sessionId);
        session.setUserId(request.userId());
        session.setLatitude(lat);
        session.setLongitude(lng);
        session.setH3Index(h3Index);
        session.setBatteryLevel(request.batteryLevel());
        session.setStatus(SosStatus.ACTIVE);
        session.setTriggeredAt(Instant.now().getEpochSecond());

        sessionRepository.save(session);   // DB write must succeed before we return

        // ── Steps B & C: Fire and forget — do NOT await these ────────────────
        // @Async means these run in a separate thread pool
        // If they fail, the session is still ACTIVE and can be retried
        alertService.alertTrustedContacts(request.userId(), sessionId,
                                          request.latitude(), request.longitude());

        alertService.alertNearbyProtectors(h3Index, sessionId,
                                           request.latitude(), request.longitude());

        // ── Return immediately — Android gets this before alerts are even sent ─
        return new SosTriggerResponse(sessionId, "ACTIVE",true,30);                         
    }

    @Transactional
    public void updateLocation(String sessionId, LocationUpdateRequest request) {

        SosSession session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setLatitude(request.lat());
        session.setLongitude(request.lng());

        // Optional: update timestamp
        session.setTriggeredAt(java.time.Instant.now().getEpochSecond());

        sessionRepository.save(session);
    }
}