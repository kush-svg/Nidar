package com.example.nidar.heatmap.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.nidar.heatmap.dto.WeightedClusterDto;
import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.repository.IncidentRepository;
import com.example.nidar.heatmap.service.BoundingBoxService;
import com.example.nidar.heatmap.service.HeatmapCacheService;
import com.example.nidar.heatmap.service.HeatmapScoringService;

import lombok.RequiredArgsConstructor;
import com.example.nidar.heatmap.dto.BoundingBox;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/heatmap")
@RequiredArgsConstructor
public class HeatmapController {
    
    private final BoundingBoxService    bboxService;
    private final IncidentRepository    incidentRepo;
    private final HeatmapScoringService scoringService;
    private final HeatmapCacheService   cacheService;

    // Called when user opens the map OR camera stops moving
    // Android sends the 4 corners of the visible map region
    @GetMapping
    public ResponseEntity<List<WeightedClusterDto>> getHeatmap(
        @RequestParam double centerLat,
        @RequestParam double centerLng,
        @RequestParam(defaultValue = "5.0") double radiusKm
    ) {
        BoundingBox bbox     = bboxService.compute(centerLat, centerLng, radiusKm);
        String      cacheKey = cacheService.buildCacheKey(bbox);

        // Cache hit — return immediately, no DB call
        Optional<List<WeightedClusterDto>> cached = cacheService.get(cacheKey);
        if (cached.isPresent()) return ResponseEntity.ok(cached.get());

        // Cache miss — query, score, cache, return
        long cutoff = Instant.now().minusSeconds(30L * 86_400).getEpochSecond();

        List<Incident> incidents = incidentRepo.findWithinBoundingBox(
            bbox.minLat(), bbox.maxLat(),
            bbox.minLng(), bbox.maxLng(),
            cutoff
        );

        List<WeightedClusterDto> clusters = scoringService.score(incidents);
        cacheService.put(cacheKey, clusters);

        return ResponseEntity.ok(clusters);
    }
}
