package com.example.nidar.evidence.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.example.nidar.evidence.repository.EvidenceRepository;

@Service
@RequiredArgsConstructor
public class HashChainService {

    private final EvidenceRepository evidenceRepository;

    // Get the most recent chainHash for this user — becomes previousHash for next item
    public String getLatestHash(String userId) {
        return evidenceRepository
            .findTopByUserIdOrderByUploadedAtDesc(userId)
            .map(e -> e.getChainHash())
            .orElse("GENESIS");   // first item in chain has no predecessor
    }

    // chainHash = SHA-256(previousHash + fileHash)
    // Altering any item breaks every subsequent chainHash
    public String computeChainHash(String previousHash, String fileHash) {
        try {
            String combined = previousHash + fileHash;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
