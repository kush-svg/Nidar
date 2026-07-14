package com.example.nidar.sos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.sos.model.SosSession;
import com.example.nidar.sos.model.SosStatus;
import com.example.nidar.sos.repository.SosSessionRepository;
import com.example.nidar.sos.dto.LocationUpdateRequest;
import com.example.nidar.sos.dto.SosTriggerRequest;
import com.example.nidar.sos.dto.SosTriggerResponse;
import com.example.nidar.common.util.H3SnapUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SosServiceTest {

    @Mock
    private SosSessionRepository sessionRepository;

    @Mock
    private SosAlertService alertService;

    @Mock
    private H3SnapUtil h3SnapUtil;

    @InjectMocks
    private SosService sosService;

    @Test
    void triggerSos_CreatesSessionAndReturnsActiveResponse() {
        SosTriggerRequest request = new SosTriggerRequest(
            "u1", 28.6139, 77.2090, 85
        );
        when(h3SnapUtil.cellIndex(28.6139, 77.2090)).thenReturn("h3cell123");

        SosTriggerResponse response = sosService.triggerSos(request);

        assertNotNull(response.sosSessionId());
        assertEquals("ACTIVE", response.status());
        assertTrue(response.startAudioCapture());
        assertEquals(30, response.locationSnapshotIntervalSeconds());
    }

    @Test
    void triggerSos_PersistsSessionWithCorrectFields() {
        SosTriggerRequest request = new SosTriggerRequest(
            "u1", 28.6139, 77.2090, 85
        );
        when(h3SnapUtil.cellIndex(28.6139, 77.2090)).thenReturn("h3cell123");

        sosService.triggerSos(request);

        ArgumentCaptor<SosSession> captor = ArgumentCaptor.forClass(SosSession.class);
        verify(sessionRepository).save(captor.capture());

        SosSession saved = captor.getValue();
        assertEquals("u1", saved.getUserId());
        assertEquals(28.6139, saved.getLatitude());
        assertEquals(77.2090, saved.getLongitude());
        assertEquals("h3cell123", saved.getH3Index());
        assertEquals(85, saved.getBatteryLevel());
        assertEquals(SosStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getTriggeredAt());
    }

    @Test
    void triggerSos_FiresAlertsTrustedContactsAsync() {
        SosTriggerRequest request = new SosTriggerRequest(
            "u1", 28.6139, 77.2090, 85
        );
        when(h3SnapUtil.cellIndex(28.6139, 77.2090)).thenReturn("h3cell123");

        SosTriggerResponse response = sosService.triggerSos(request);

        verify(alertService).alertTrustedContacts(
            eq("u1"), eq(response.sosSessionId()),
            eq(28.6139), eq(77.2090)
        );
    }

    @Test
    void triggerSos_FiresAlertsNearbyProtectorsAsync() {
        SosTriggerRequest request = new SosTriggerRequest(
            "u1", 28.6139, 77.2090, 85
        );
        when(h3SnapUtil.cellIndex(28.6139, 77.2090)).thenReturn("h3cell123");

        SosTriggerResponse response = sosService.triggerSos(request);

        verify(alertService).alertNearbyProtectors(
            eq("h3cell123"), eq(response.sosSessionId()),
            eq(28.6139), eq(77.2090)
        );
    }

    @Test
    void updateLocation_WhenSessionExists_UpdatesLatLng() {
        SosSession session = new SosSession();
        session.setSessionId("s1");
        session.setLatitude(28.6139);
        session.setLongitude(77.2090);

        when(sessionRepository.findBySessionId("s1")).thenReturn(Optional.of(session));

        LocationUpdateRequest request = new LocationUpdateRequest(28.7000, 77.3000, 10.0f, 80);

        sosService.updateLocation("s1", request);

        assertEquals(28.7000, session.getLatitude());
        assertEquals(77.3000, session.getLongitude());
        verify(sessionRepository).save(session);
    }

    @Test
    void updateLocation_WhenSessionDoesNotExist_ThrowsException() {
        when(sessionRepository.findBySessionId("nonexistent")).thenReturn(Optional.empty());

        LocationUpdateRequest request = new LocationUpdateRequest(28.7000, 77.3000, 10.0f, 80);

        assertThrows(RuntimeException.class,
            () -> sosService.updateLocation("nonexistent", request));
    }
}
