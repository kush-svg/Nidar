package com.example.nidar.heatmap.service;

import com.example.nidar.common.util.H3SnapUtil;
import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NcrbDataServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    private H3SnapUtil h3SnapUtil;

    @InjectMocks
    private NcrbDataService ncrbDataService;

    @Captor
    private ArgumentCaptor<List<Incident>> incidentsCaptor;

    @BeforeEach
    void setUp() throws Exception {
        h3SnapUtil = new H3SnapUtil(); // Use real H3 utility
        ncrbDataService = new NcrbDataService(incidentRepository, h3SnapUtil);
    }

    @Test
    void testIngestNcrbData() throws Exception {
        // Run the sync (this will use the dummy records hardcoded for Delhi, Faridabad, etc.)
        ncrbDataService.ingestNcrbData();

        // Verify that incidentRepository.saveAll() was called multiple times (once per record)
        verify(incidentRepository, atLeastOnce()).saveAll(incidentsCaptor.capture());

        // Let's inspect the saved incidents
        List<List<Incident>> allSavedBatches = incidentsCaptor.getAllValues();
        int totalInserted = 0;
        
        for (List<Incident> batch : allSavedBatches) {
            totalInserted += batch.size();
            for (Incident incident : batch) {
                // Ensure the properties of NCRB incidents are correct
                assertEquals("ncrb-system", incident.getUserId());
                assertEquals(0.8, incident.getTrustScore(), 0.001); // High trust score for NCRB
                assertNotNull(incident.getH3Index());
                assertNotNull(incident.getIncidentType());
                assertTrue(incident.getLatitude() > 0); // Northern hemisphere (India)
                assertTrue(incident.getLongitude() > 0); // Eastern hemisphere (India)
            }
        }

        // We expect several thousand incidents distributed across cells
        // (Delhi rape=1340, assault=4921, etc.)
        assertTrue(totalInserted > 0, "Should have inserted multiple incidents");
        System.out.println("Successfully generated and distributed " + totalInserted + " incidents across H3 cells.");
    }
}
