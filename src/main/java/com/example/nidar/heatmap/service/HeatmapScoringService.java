package com.example.nidar.heatmap.service;

import org.springframework.stereotype.Service;

import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.dto.WeightedClusterDto;

import java.time.Instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@Service
public class HeatmapScoringService {

    private static final double DECAY_RATE      = 0.90;   // 10% weight loss per day
    private static final double CLUSTER_RADIUS_M = 100.0; // merge within 100 meters
    private static final double MAX_SCORE        = 100.0;  // cap for UI gradient
    private static final long   SECONDS_PER_DAY  = 86_400L;

    public List<WeightedClusterDto> score(List<Incident> incidents) {
        List<ActiveCluster> clusters = cluster(incidents);  // Stage A
        return clusters.stream()
            .map(this::computeDangerScore)                  // Stages B + C
            .map(this::toDto)
            .toList();
    }

    // ── Stage A: Radius clustering ────────────────────────────────────────────
    // Implements your document's algorithm loop exactly:
    // iterate → check proximity → merge OR create new cluster
    private List<ActiveCluster> cluster(List<Incident> incidents) {
        List<ActiveCluster> activeClusters = new ArrayList<>();
        long now = Instant.now().getEpochSecond();

        for (Incident incident : incidents) {
            ActiveCluster nearest = findNearestCluster(activeClusters, incident);

            if (nearest != null) {
                nearest.merge(incident, now);   // pull center toward new point
            } else {
                activeClusters.add(new ActiveCluster(incident, now));
            }
        }
        return activeClusters;
    }

    private ActiveCluster findNearestCluster(List<ActiveCluster> clusters, Incident incident) {
        return clusters.stream()
            .filter(c -> haversineMeters(c.centerLat, c.centerLng,
                                         incident.getLatitude(), incident.getLongitude())
                         <= CLUSTER_RADIUS_M)
            .min(Comparator.comparingDouble(c ->
                haversineMeters(c.centerLat, c.centerLng,
                                incident.getLatitude(), incident.getLongitude())))
            .orElse(null);
    }

    // ── Stage B: Time decay per incident ─────────────────────────────────────
    // W_current = W_base × (0.90)^t   where t = days since incident
    private double applyTimeDecay(Incident incident, long nowEpoch) {
        long daysPassed = (nowEpoch - incident.getTimestamp()) / SECONDS_PER_DAY;
        double decayed  = incident.getBaseWeight() * Math.pow(DECAY_RATE, daysPassed);

        // Trust score multiplier — new user reports count less
        return decayed * incident.getTrustScore();
    }

    // ── Stage C: Final danger score ───────────────────────────────────────────
    // Final = (Σ W_current_i) × (1 + 0.2 × log10(N))
    private ScoredCluster computeDangerScore(ActiveCluster cluster) {
        long now = Instant.now().getEpochSecond();

        double rawSum = cluster.incidents.stream()
            .mapToDouble(i -> applyTimeDecay(i, now))
            .sum();

        int n = cluster.incidents.size();
        double densityMultiplier = 1.0 + (0.2 * Math.log10(Math.max(n, 1)));

        double finalScore = Math.min(rawSum * densityMultiplier, MAX_SCORE);

        return new ScoredCluster(cluster.centerLat, cluster.centerLng, finalScore);
    }

    // ── Haversine (accounts for Earth's curvature) ────────────────────────────
    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private WeightedClusterDto toDto(ScoredCluster sc) {
        return new WeightedClusterDto(sc.lat, sc.lng, sc.score);
    }

    // ── Inner types ────────────────────────────────────────────────────────────
    static class ActiveCluster {
        double centerLat, centerLng;
        List<Incident> incidents = new ArrayList<>();

        ActiveCluster(Incident first, long now) {
            this.centerLat = first.getLatitude();
            this.centerLng = first.getLongitude();
            this.incidents.add(first);
        }

        void merge(Incident incident, long now) {
            incidents.add(incident);
            // Pull cluster center toward the new point (running average)
            int n = incidents.size();
            centerLat = centerLat + (incident.getLatitude()  - centerLat) / n;
            centerLng = centerLng + (incident.getLongitude() - centerLng) / n;
        }
    }

    record ScoredCluster(double lat, double lng, double score) {}
}
