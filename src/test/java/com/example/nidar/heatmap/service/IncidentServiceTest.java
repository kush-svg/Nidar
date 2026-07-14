package com.example.nidar.heatmap.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.model.IncidentType;
import com.example.nidar.heatmap.repository.IncidentRepository;
import com.example.nidar.heatmap.dto.IncidentReportRequest;
import com.example.nidar.common.util.H3SnapUtil;
import com.example.nidar.common.messaging.KafkaMessagingService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private H3SnapUtil h3SnapUtil;

    @Mock
    private HeatmapCacheService cacheService;

    @Mock
    private TrustScoreService trustScoreService;

    @Mock
    private KafkaMessagingService kafkaMessagingService;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void report_ValidRequest_PersistsIncident() {
        IncidentReportRequest request = new IncidentReportRequest(
            28.6139, 77.2090, "STALKING", null
        );

        when(h3SnapUtil.snapToCell(28.6139, 77.2090))
            .thenReturn(new double[]{28.614, 77.209});
        when(h3SnapUtil.cellIndex(28.6139, 77.2090))
            .thenReturn("h3cell123");
        when(trustScoreService.calculateTrustScore("u1")).thenReturn(0.8);
        when(incidentRepository.existsByUserIdAndH3IndexAndTimestampAfter(
            eq("u1"), eq("h3cell123"), anyLong())).thenReturn(false);
        when(incidentRepository.save(any(Incident.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        Incident result = incidentService.report(request, "u1");

        assertNotNull(result);
        assertEquals(IncidentType.STALKING, result.getIncidentType());
        assertEquals(75, result.getBaseWeight()); // STALKING base weight
        assertEquals(0.8, result.getTrustScore());
        assertEquals("h3cell123", result.getH3Index());
    }

    @Test
    void report_InvalidatesCacheAfterSave() {
        IncidentReportRequest request = new IncidentReportRequest(
            28.6139, 77.2090, "POOR_LIGHTING", null
        );

        when(h3SnapUtil.snapToCell(anyDouble(), anyDouble()))
            .thenReturn(new double[]{28.614, 77.209});
        when(h3SnapUtil.cellIndex(anyDouble(), anyDouble()))
            .thenReturn("h3cell123");
        when(trustScoreService.calculateTrustScore("u1")).thenReturn(0.5);
        when(incidentRepository.existsByUserIdAndH3IndexAndTimestampAfter(
            any(), any(), anyLong())).thenReturn(false);
        when(incidentRepository.save(any(Incident.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        incidentService.report(request, "u1");

        verify(cacheService).invalidateArea("h3cell123");
    }

    @Test
    void report_PublishesToPubSub() {
        IncidentReportRequest request = new IncidentReportRequest(
            28.6139, 77.2090, "PHYSICAL_HARASSMENT", null
        );

        when(h3SnapUtil.snapToCell(anyDouble(), anyDouble()))
            .thenReturn(new double[]{28.614, 77.209});
        when(h3SnapUtil.cellIndex(anyDouble(), anyDouble()))
            .thenReturn("h3cell123");
        when(trustScoreService.calculateTrustScore("u1")).thenReturn(0.5);
        when(incidentRepository.existsByUserIdAndH3IndexAndTimestampAfter(
            any(), any(), anyLong())).thenReturn(false);
        when(incidentRepository.save(any(Incident.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        incidentService.report(request, "u1");

        verify(kafkaMessagingService).publishLocationIncident(
            eq("h3cell123"), eq("PHYSICAL_HARASSMENT"),
            eq(28.6139), eq(77.2090)
        );
    }

    @Test
    void report_DuplicateInSameArea_ThrowsException() {
        IncidentReportRequest request = new IncidentReportRequest(
            28.6139, 77.2090, "STALKING", null
        );

        when(h3SnapUtil.snapToCell(anyDouble(), anyDouble()))
            .thenReturn(new double[]{28.614, 77.209});
        when(h3SnapUtil.cellIndex(anyDouble(), anyDouble()))
            .thenReturn("h3cell123");
        when(trustScoreService.calculateTrustScore("u1")).thenReturn(0.8);
        when(incidentRepository.existsByUserIdAndH3IndexAndTimestampAfter(
            eq("u1"), eq("h3cell123"), anyLong())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> incidentService.report(request, "u1"));
        assertTrue(ex.getMessage().contains("already reported"));
    }

    @Test
    void report_UsesSnappedCoordinates_NotRawInput() {
        IncidentReportRequest request = new IncidentReportRequest(
            28.6139, 77.2090, "DESERTED_AREA", null
        );

        // snapToCell returns different (privacy-snapped) coordinates
        when(h3SnapUtil.snapToCell(28.6139, 77.2090))
            .thenReturn(new double[]{28.6100, 77.2050});
        when(h3SnapUtil.cellIndex(28.6139, 77.2090))
            .thenReturn("h3cell123");
        when(trustScoreService.calculateTrustScore("u1")).thenReturn(0.5);
        when(incidentRepository.existsByUserIdAndH3IndexAndTimestampAfter(
            any(), any(), anyLong())).thenReturn(false);
        when(incidentRepository.save(any(Incident.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        Incident result = incidentService.report(request, "u1");

        // Saved coordinates should be the snapped ones, not the raw input
        assertEquals(28.6100, result.getLatitude());
        assertEquals(77.2050, result.getLongitude());
    }
}
