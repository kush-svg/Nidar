package com.example.nidar.home.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.nidar.common.util.H3SnapUtil;
import com.example.nidar.home.dto.HomeSummaryDto;
import com.example.nidar.home.dto.NearbyProtectorDto;
import com.example.nidar.heatmap.dto.WeightedClusterDto;
import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.repository.IncidentRepository;
import com.example.nidar.auth.model.User;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.heatmap.service.HeatmapScoringService;
import com.example.nidar.heatmap.service.HeatmapCacheService;
import com.example.nidar.common.service.GeocodingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeService {

    private final IncidentRepository   incidentRepository;
    private final UserRepository        userRepository;
    private final H3SnapUtil            h3SnapUtil;
    private final HeatmapScoringService scoringService;
    private final HeatmapCacheService   cacheService;
    private final GeocodingService      geocodingService;

    public HomeSummaryDto getSummary(double lat, double lng) {

        // Get H3 cell for user's location
        String userCell = h3SnapUtil.cellIndex(lat, lng);

        // Get neighboring cells for area score
        // gridDisk returns unmodifiable list — wrap it so we can add userCell
        List<String> searchCells = new ArrayList<>(h3SnapUtil.getNeighborCells(userCell, 1));
        searchCells.add(userCell);

        // Compute danger score for user's area
        long cutoff = Instant.now().minusSeconds(30L * 86_400).getEpochSecond();
        List<Incident> incidents = incidentRepository
            .findByH3IndexInAndTimestampAfter(searchCells, cutoff);

        // Cache-first: reuse already-scored clusters if Redis has them
        String cacheKey = cacheService.buildCacheKey(searchCells);
        List<WeightedClusterDto> clusters = cacheService.get(cacheKey)
            .orElseGet(() -> {
                List<WeightedClusterDto> computed = scoringService.score(incidents);
                cacheService.put(cacheKey, computed);
                return computed;
            });

        // Average score across clusters in the area
        double avgScore = clusters.stream()
            .mapToDouble(c -> c.dangerScore())
            .average()
            .orElse(0.0);

        int riskScore = (int) Math.min(avgScore, 100);
        String riskLevel = resolveRiskLevel(riskScore);

        // Find nearby active protectors
        long thirtyMinutesAgo = Instant.now().minusSeconds(1800).getEpochSecond();
        List<User> protectors = userRepository
            .findActiveProtectorsInCells(searchCells, thirtyMinutesAgo);

        List<NearbyProtectorDto> nearbyProtectors = protectors.stream()
            .map(p -> new NearbyProtectorDto(
                p.getName(),
                haversineKm(lat, lng, extractLat(p.getH3Index()),
                                       extractLng(p.getH3Index())),
                p.getH3Index()
            ))
            .sorted(Comparator.comparingDouble(p2 -> p2.distanceKm()))
            .limit(5)
            .toList();

        String locationName = geocodingService.reverseGeocode(lat, lng);

        return new HomeSummaryDto(
            riskScore,
            riskLevel,
            locationName,
            protectors.size(),
            nearbyProtectors
        );
    }

    private String resolveRiskLevel(int score) {
        if (score <= 20)  return "SAFE";
        if (score <= 60)  return "CAUTION";
        return "DANGER";
    }

    private double haversineKm(double lat1, double lng1,
                                double lat2, double lng2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1))
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    // Delegates to H3SnapUtil which holds the singleton H3Core instance
    private double extractLat(String h3Index) {
        try { return h3SnapUtil.cellToCenter(h3Index)[0]; }
        catch (Exception e) { return 0.0; }
    }

    private double extractLng(String h3Index) {
        try { return h3SnapUtil.cellToCenter(h3Index)[1]; }
        catch (Exception e) { return 0.0; }
    }
}
