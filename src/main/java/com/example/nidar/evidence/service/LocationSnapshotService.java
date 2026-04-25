package com.example.nidar.evidence.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.example.nidar.evidence.model.LocationSnapshot;

import com.example.nidar.evidence.repository.LocationSnapshotRepository;
@Service
@RequiredArgsConstructor
public class LocationSnapshotService {

    private final LocationSnapshotRepository snapshotRepository;
    private final HashChainService           hashChainService;

    // Called every 30 seconds by Android during active SOS
    public void record(String sosSessionId, String userId,
                       double lat, double lng, float accuracy) {

        String data       = lat + "," + lng + "," + accuracy;
        String sha256     = computeSha256(data);
        String prevHash   = getLatestSnapshotHash(sosSessionId);
        String chainHash  = hashChainService.computeChainHash(prevHash, sha256);

        LocationSnapshot snapshot = LocationSnapshot.builder()
            .id(UUID.randomUUID().toString())
            .sosSessionId(sosSessionId)
            .userId(userId)
            .latitude(lat)
            .longitude(lng)
            .accuracy(accuracy)
            .capturedAt(Instant.now().getEpochSecond())
            .sha256Hash(sha256)
            .previousHash(prevHash)
            .chainHash(chainHash)
            .build();

        snapshotRepository.save(snapshot);
    }

    private String getLatestSnapshotHash(String sosSessionId) {
        return snapshotRepository
            .findTopBySosSessionIdOrderByCapturedAtDesc(sosSessionId)
            .map(LocationSnapshot::getChainHash)
            .orElse("GENESIS");
    }

    private String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
