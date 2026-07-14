package com.example.nidar.heatmap.service;

import org.junit.jupiter.api.Test;
import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.dto.WeightedClusterDto;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeatmapScoringServiceTest {

    private final HeatmapScoringService scoringService = new HeatmapScoringService();

    @Test
    void testClusteringByProximity() {
        // Two incidents very close (approx. 20-30 meters apart)
        // Center: Delhi area (28.6139, 77.2090)
        Incident i1 = Incident.builder()
                .latitude(28.6139)
                .longitude(77.2090)
                .baseWeight(10)
                .trustScore(1.0)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        Incident i2 = Incident.builder()
                .latitude(28.6141) // slightly north
                .longitude(77.2091)
                .baseWeight(15)
                .trustScore(1.0)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        // One incident far away (approx 20 km away - Gurgaon)
        Incident i3 = Incident.builder()
                .latitude(28.4595)
                .longitude(77.0266)
                .baseWeight(20)
                .trustScore(1.0)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        List<WeightedClusterDto> result = scoringService.score(List.of(i1, i2, i3));

        // We expect exactly 2 clusters:
        // Cluster 1: i1 and i2 grouped together
        // Cluster 2: i3 by itself
        assertEquals(2, result.size());

        // Validate that the Delhi cluster has both merged (center should be average lat/lng)
        WeightedClusterDto delhiCluster = result.stream()
                .filter(c -> Math.abs(c.lat() - 28.6139) < 0.01)
                .findFirst()
                .orElseThrow();

        assertEquals((28.6139 + 28.6141) / 2.0, delhiCluster.lat(), 0.0001);
        assertEquals((77.2090 + 77.2091) / 2.0, delhiCluster.lng(), 0.0001);

        // Validate the Gurgaon cluster
        WeightedClusterDto gurgaonCluster = result.stream()
                .filter(c -> Math.abs(c.lat() - 28.4595) < 0.01)
                .findFirst()
                .orElseThrow();

        assertEquals(28.4595, gurgaonCluster.lat(), 0.0001);
        assertEquals(77.0266, gurgaonCluster.lng(), 0.0001);
    }

    @Test
    void testTimeDecay() {
        long now = Instant.now().getEpochSecond();
        long tenDaysAgo = now - (10L * 86400L); // 10 days in seconds

        Incident recentIncident = Incident.builder()
                .latitude(28.6139)
                .longitude(77.2090)
                .baseWeight(50)
                .trustScore(1.0)
                .timestamp(now)
                .build();

        Incident oldIncident = Incident.builder()
                .latitude(28.4595)
                .longitude(77.0266)
                .baseWeight(50)
                .trustScore(1.0)
                .timestamp(tenDaysAgo)
                .build();

        List<WeightedClusterDto> result = scoringService.score(List.of(recentIncident, oldIncident));
        assertEquals(2, result.size());

        WeightedClusterDto recentCluster = result.stream()
                .filter(c -> Math.abs(c.lat() - 28.6139) < 0.01)
                .findFirst()
                .orElseThrow();

        WeightedClusterDto oldCluster = result.stream()
                .filter(c -> Math.abs(c.lat() - 28.4595) < 0.01)
                .findFirst()
                .orElseThrow();

        // Recent incident danger score should be close to baseWeight * trustScore = 50.0
        assertEquals(50.0, recentCluster.dangerScore(), 1.0);

        // Old incident danger score should be significantly decayed: 50 * (0.90)^10 = ~17.43
        double expectedOldScore = 50.0 * Math.pow(0.90, 10);
        assertEquals(expectedOldScore, oldCluster.dangerScore(), 1.0);
        assertTrue(recentCluster.dangerScore() > oldCluster.dangerScore());
    }

    @Test
    void testDensityMultiplierAndCap() {
        long now = Instant.now().getEpochSecond();

        // 10 incidents at the exact same location with baseWeight 15 each
        // Combined weight raw = 150
        // Density multiplier for n=10: 1 + 0.2 * log10(10) = 1.2
        // Capped score must not exceed 100.0
        Incident.IncidentBuilder builder = Incident.builder()
                .latitude(28.6139)
                .longitude(77.2090)
                .baseWeight(15)
                .trustScore(1.0)
                .timestamp(now);

        List<Incident> incidents = List.of(
                builder.build(), builder.build(), builder.build(), builder.build(), builder.build(),
                builder.build(), builder.build(), builder.build(), builder.build(), builder.build()
        );

        List<WeightedClusterDto> result = scoringService.score(incidents);
        assertEquals(1, result.size());

        WeightedClusterDto cluster = result.get(0);
        // Should be exactly capped at 100.0
        assertEquals(100.0, cluster.dangerScore(), 0.001);
    }
}
