package com.example.nidar.evidence.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

import com.example.nidar.evidence.model.EvidenceItem;
import com.example.nidar.evidence.model.EvidenceStatus;

import com.example.nidar.evidence.dto.EvidenceItemDto;
import com.example.nidar.auth.service.JwtService;

import com.example.nidar.evidence.repository.EvidenceRepository;
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceAccessService {

    private final EvidenceRepository   evidenceRepository;
    private final MinioStorageService  minioStorageService;
    private final EvidenceAuditService auditService;
    private final JwtService           jwtService;

public void validateVaultToken(String userId, String vaultToken) {
    if (vaultToken == null || vaultToken.isBlank()) {
        throw new RuntimeException("Vault token required");
    }

    // Validate using JwtService — vault token uses separate secret
    if (!jwtService.validateVaultToken(vaultToken)) {
        throw new RuntimeException("Invalid or expired vault token");
    }

    // Verify token belongs to this user
    String tokenUserId = jwtService.extractUserIdFromVaultToken(vaultToken);
    if (!tokenUserId.equals(userId)) {
        throw new RuntimeException("Vault token does not belong to this user");
    }
}

    // Returns all confirmed + pending items for the owner
    public List<EvidenceItemDto> getVaultItems(String userId) {
        return evidenceRepository
            .findByUserIdOrderByUploadedAtDesc(userId)
            .stream()
            .filter(e -> e.getStatus() != EvidenceStatus.DELETED)
            .map(EvidenceItemDto::from)
            .toList();
    }

    // Generates a 15-minute pre-signed URL — logs the access
    public String generatePresignedUrl(String itemId, String vaultToken) {
        EvidenceItem item = evidenceRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Evidence item not found: " + itemId));

        // Ownership check — no cross-user access ever
        validateVaultToken(item.getUserId(), vaultToken);

        String url = minioStorageService.generatePresignedUrl(item.getMinioObjectKey());

        // Every download is logged — court can see full access history
        auditService.log(item.getId(), item.getUserId(),
                         item.getSha256Hash(), "ACCESSED");

        return url;
    }

    // User reviews an auto-captured PENDING_REVIEW item
    @Transactional
    public void processReview(String itemId, String decision, String vaultToken) {
        EvidenceItem item = evidenceRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Evidence item not found: " + itemId));

        validateVaultToken(item.getUserId(), vaultToken);

        if (item.getStatus() != EvidenceStatus.PENDING_REVIEW) {
            throw new RuntimeException("Item is not pending review: " + itemId);
        }

        if ("KEEP".equalsIgnoreCase(decision)) {
            item.setStatus(EvidenceStatus.CONFIRMED);
            evidenceRepository.save(item);
            auditService.log(item, "CONFIRMED");

        } else if ("DELETE".equalsIgnoreCase(decision)) {
            item.setStatus(EvidenceStatus.DELETED);
            evidenceRepository.save(item);
            minioStorageService.delete(item.getMinioObjectKey());   // wipe from storage
            auditService.log(item, "DELETED");

        } else {
            throw new RuntimeException("Invalid decision: " + decision);
        }
    }

    // Cleanup job — auto-deletes PENDING_REVIEW items older than 24 hours
    // Run via @Scheduled every hour
    @Transactional
    public void expirePendingItems() {
        long cutoff = Instant.now().minusSeconds(86_400).getEpochSecond();

        List<EvidenceItem> expired = evidenceRepository.findExpiredPendingItems(cutoff);

        for (EvidenceItem item : expired) {
            item.setStatus(EvidenceStatus.DELETED);
            evidenceRepository.save(item);
            minioStorageService.delete(item.getMinioObjectKey());
            auditService.log(item, "AUTO_EXPIRED");
            log.info("Auto-expired pending evidence item: {}", item.getId());
        }
    }
}
