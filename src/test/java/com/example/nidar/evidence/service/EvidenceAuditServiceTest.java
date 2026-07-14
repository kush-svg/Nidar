package com.example.nidar.evidence.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.evidence.model.AuditLog;
import com.example.nidar.evidence.model.EvidenceItem;
import com.example.nidar.evidence.repository.AuditLogRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceAuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private EvidenceAuditService evidenceAuditService;

    @Test
    void log_WithEvidenceItem_PersistsAuditEntry() {
        EvidenceItem item = EvidenceItem.builder()
            .id("e1").userId("u1")
            .sha256Hash("abc123").chainHash("chain456")
            .build();

        evidenceAuditService.log(item, "UPLOADED");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("e1", saved.getEvidenceItemId());
        assertEquals("u1", saved.getUserId());
        assertEquals("UPLOADED", saved.getAction());
        assertEquals("abc123", saved.getFileHash());
        assertEquals("chain456", saved.getChainHash());
        assertNotNull(saved.getPerformedAt());
    }

    @Test
    void log_WithStringParams_PersistsAuditEntry() {
        evidenceAuditService.log("e1", "u1", "fileHash123", "ACCESSED");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("e1", saved.getEvidenceItemId());
        assertEquals("u1", saved.getUserId());
        assertEquals("ACCESSED", saved.getAction());
        assertEquals("fileHash123", saved.getFileHash());
        assertNull(saved.getChainHash()); // string overload does not set chainHash
    }

    @Test
    void log_GeneratesUniqueIdsEachCall() {
        EvidenceItem item = EvidenceItem.builder()
            .id("e1").userId("u1").sha256Hash("h").chainHash("c").build();

        evidenceAuditService.log(item, "UPLOADED");
        evidenceAuditService.log(item, "ACCESSED");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(captor.capture());

        String id1 = captor.getAllValues().get(0).getId();
        String id2 = captor.getAllValues().get(1).getId();
        assertNotEquals(id1, id2);
    }
}
