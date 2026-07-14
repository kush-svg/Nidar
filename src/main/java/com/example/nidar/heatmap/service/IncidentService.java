package com.example.nidar.heatmap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;


import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.model.IncidentType;
import com.example.nidar.heatmap.repository.IncidentRepository;
import com.example.nidar.heatmap.dto.IncidentReportRequest;
import com.example.nidar.common.util.H3SnapUtil;
import com.example.nidar.common.messaging.KafkaMessagingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final H3SnapUtil         h3SnapUtil;
    private final HeatmapCacheService cacheService;
    private final TrustScoreService   trustScoreService;
    private final KafkaMessagingService kafkaMessagingService;



    @Transactional
    public Incident report(IncidentReportRequest request, String userId) {

        double[] snapped = h3SnapUtil.snapToCell(
            request.latitude(), request.longitude()
        );
        String h3Index = h3SnapUtil.cellIndex(
            request.latitude(), request.longitude()
        );

        IncidentType type = IncidentType.valueOf(request.incidentType());

        // Calculate trust score dynamically
        double trustScore = trustScoreService.calculateTrustScore(userId);


        boolean isDuplicate = incidentRepository
            .existsByUserIdAndH3IndexAndTimestampAfter(
                userId,
                h3Index,
                Instant.now().minusSeconds(3600).getEpochSecond()
            );

        if (isDuplicate) {
            throw new RuntimeException(
                "You already reported an incident in this area recently"
            );
        }
        
        Incident incident = Incident.builder()
            .userId(userId)
            .latitude(snapped[0])
            .longitude(snapped[1])
            .h3Index(h3Index)
            .incidentType(type)
            .baseWeight(type.getBaseWeight())
            .trustScore(trustScore)           // dynamic now
            .timestamp(Instant.now().getEpochSecond())
            .build();

        incidentRepository.save(incident);
        cacheService.invalidateArea(h3Index);

        // Publish to global Pub/Sub for real-time aggregation/analytics
        kafkaMessagingService.publishLocationIncident(
            h3Index, request.incidentType(), request.latitude(), request.longitude()
        );

        log.info("Incident reported by {} (trust: {}) at h3={}",
            userId, trustScore, h3Index);
        return incident;
    }
}
