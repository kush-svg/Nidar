package com.example.nidar.heatmap.service;

import com.example.nidar.heatmap.dto.BoundingBox;
import com.example.nidar.heatmap.dto.WeightedClusterDto;
import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.repository.IncidentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapScheduler {

    private final IncidentRepository    incidentRepository;
    private final HeatmapScoringService scoringService;
    private final HeatmapCacheService   cacheService;
    private final BoundingBoxService    bboxService;

    // Refresh pre-computed tiles every 5 minutes
    @Scheduled(fixedRate = 300_000)
    public void refreshHeatmapCache() {
        log.debug("Refreshing heatmap cache...");

        // Pre-compute for major NCR cities
        List<double[]> majorCenters = List.of(
            new double[]{28.4089, 77.3178},  // Faridabad
            new double[]{28.4595, 77.0266},  // Gurugram
            new double[]{28.6139, 77.2090},  // Delhi center
            new double[]{28.5355, 77.3910},  // Noida
            new double[]{28.6692, 77.4538}   // Ghaziabad
        );

        long cutoff = Instant.now()
            .minusSeconds(30L * 86_400)
            .getEpochSecond();

        for (double[] center : majorCenters) {
            try {
                BoundingBox bbox =
                    bboxService.compute(center[0], center[1], 10.0);

                List<Incident> incidents = incidentRepository
                    .findWithinBoundingBox(
                        bbox.minLat(), bbox.maxLat(),
                        bbox.minLng(), bbox.maxLng(),
                        cutoff
                    );

                List<WeightedClusterDto> clusters =
                    scoringService.score(incidents);

                String key = cacheService.buildCacheKey(bbox);
                cacheService.put(key, clusters);

            } catch (Exception e) {
                log.warn("Cache refresh failed for center {},{}: {}",
                         center[0], center[1], e.getMessage());
            }
        }

        log.debug("Heatmap cache refreshed for {} centers", majorCenters.size());
    }
}
