package com.example.nidar.evidence.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.UUID;
import com.example.nidar.evidence.model.EvidenceItem;
import com.example.nidar.evidence.model.AuditLog;
import com.example.nidar.evidence.repository.AuditLogRepository;
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceAuditService {

    private final AuditLogRepository auditLogRepository;

    // Every action on an evidence item is logged — immutably
    // This is the Section 65B paper trail
    public void log(EvidenceItem item, String action) {
        AuditLog entry = AuditLog.builder()
            .id(UUID.randomUUID().toString())
            .evidenceItemId(item.getId())
            .userId(item.getUserId())
            .action(action)                          // UPLOADED, ACCESSED, DELETED, CONFIRMED
            .performedAt(Instant.now().getEpochSecond())
            .fileHash(item.getSha256Hash())
            .chainHash(item.getChainHash())
            .build();

        auditLogRepository.save(entry);
    }

    public void log(String evidenceItemId, String userId,
                    String fileHash, String action) {
        AuditLog entry = AuditLog.builder()
            .id(UUID.randomUUID().toString())
            .evidenceItemId(evidenceItemId)
            .userId(userId)
            .action(action)
            .performedAt(Instant.now().getEpochSecond())
            .fileHash(fileHash)
            .build();

        auditLogRepository.save(entry);
    }
}
