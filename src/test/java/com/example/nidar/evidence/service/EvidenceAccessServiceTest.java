package com.example.nidar.evidence.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.evidence.model.EvidenceItem;
import com.example.nidar.evidence.model.EvidenceStatus;
import com.example.nidar.evidence.repository.EvidenceRepository;
import com.example.nidar.auth.service.JwtService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceAccessServiceTest {

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private EvidenceAuditService auditService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private EvidenceAccessService evidenceAccessService;

    @Test
    void validateVaultToken_WhenTokenIsNull_ThrowsException() {
        assertThrows(RuntimeException.class,
            () -> evidenceAccessService.validateVaultToken("u1", null));
    }

    @Test
    void validateVaultToken_WhenTokenIsBlank_ThrowsException() {
        assertThrows(RuntimeException.class,
            () -> evidenceAccessService.validateVaultToken("u1", "   "));
    }

    @Test
    void validateVaultToken_WhenTokenInvalid_ThrowsException() {
        when(jwtService.validateVaultToken("bad-token")).thenReturn(false);

        assertThrows(RuntimeException.class,
            () -> evidenceAccessService.validateVaultToken("u1", "bad-token"));
    }

    @Test
    void validateVaultToken_WhenTokenBelongsToDifferentUser_ThrowsException() {
        when(jwtService.validateVaultToken("token")).thenReturn(true);
        when(jwtService.extractUserIdFromVaultToken("token")).thenReturn("other-user");

        assertThrows(RuntimeException.class,
            () -> evidenceAccessService.validateVaultToken("u1", "token"));
    }

    @Test
    void validateVaultToken_WhenValidAndMatchingUser_DoesNotThrow() {
        when(jwtService.validateVaultToken("token")).thenReturn(true);
        when(jwtService.extractUserIdFromVaultToken("token")).thenReturn("u1");

        assertDoesNotThrow(
            () -> evidenceAccessService.validateVaultToken("u1", "token"));
    }

    @Test
    void getVaultItems_FiltersOutDeletedItems() {
        EvidenceItem confirmed = EvidenceItem.builder()
            .id("e1").userId("u1").status(EvidenceStatus.CONFIRMED)
            .sha256Hash("h1").build();
        EvidenceItem pending = EvidenceItem.builder()
            .id("e2").userId("u1").status(EvidenceStatus.PENDING_REVIEW)
            .sha256Hash("h2").build();
        EvidenceItem deleted = EvidenceItem.builder()
            .id("e3").userId("u1").status(EvidenceStatus.DELETED)
            .sha256Hash("h3").build();

        when(evidenceRepository.findByUserIdOrderByUploadedAtDesc("u1"))
            .thenReturn(List.of(confirmed, pending, deleted));

        var items = evidenceAccessService.getVaultItems("u1");

        assertEquals(2, items.size()); // deleted is filtered out
    }

    @Test
    void processReview_KeepDecision_ConfirmsItem() {
        EvidenceItem item = EvidenceItem.builder()
            .id("e1").userId("u1").status(EvidenceStatus.PENDING_REVIEW)
            .sha256Hash("h").build();

        when(evidenceRepository.findById("e1")).thenReturn(Optional.of(item));
        when(jwtService.validateVaultToken("token")).thenReturn(true);
        when(jwtService.extractUserIdFromVaultToken("token")).thenReturn("u1");

        evidenceAccessService.processReview("e1", "KEEP", "token");

        assertEquals(EvidenceStatus.CONFIRMED, item.getStatus());
        verify(evidenceRepository).save(item);
        verify(auditService).log(item, "CONFIRMED");
    }

    @Test
    void processReview_DeleteDecision_DeletesFromStorageAndMarksDeleted() {
        EvidenceItem item = EvidenceItem.builder()
            .id("e1").userId("u1").status(EvidenceStatus.PENDING_REVIEW)
            .minioObjectKey("u1/2026/07/uuid1").sha256Hash("h").build();

        when(evidenceRepository.findById("e1")).thenReturn(Optional.of(item));
        when(jwtService.validateVaultToken("token")).thenReturn(true);
        when(jwtService.extractUserIdFromVaultToken("token")).thenReturn("u1");

        evidenceAccessService.processReview("e1", "DELETE", "token");

        assertEquals(EvidenceStatus.DELETED, item.getStatus());
        verify(minioStorageService).delete("u1/2026/07/uuid1");
        verify(auditService).log(item, "DELETED");
    }

    @Test
    void processReview_InvalidDecision_ThrowsException() {
        EvidenceItem item = EvidenceItem.builder()
            .id("e1").userId("u1").status(EvidenceStatus.PENDING_REVIEW)
            .sha256Hash("h").build();

        when(evidenceRepository.findById("e1")).thenReturn(Optional.of(item));
        when(jwtService.validateVaultToken("token")).thenReturn(true);
        when(jwtService.extractUserIdFromVaultToken("token")).thenReturn("u1");

        assertThrows(RuntimeException.class,
            () -> evidenceAccessService.processReview("e1", "INVALID", "token"));
    }

    @Test
    void processReview_WhenNotPendingReview_ThrowsException() {
        EvidenceItem item = EvidenceItem.builder()
            .id("e1").userId("u1").status(EvidenceStatus.CONFIRMED)
            .sha256Hash("h").build();

        when(evidenceRepository.findById("e1")).thenReturn(Optional.of(item));
        when(jwtService.validateVaultToken("token")).thenReturn(true);
        when(jwtService.extractUserIdFromVaultToken("token")).thenReturn("u1");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> evidenceAccessService.processReview("e1", "KEEP", "token"));
        assertTrue(ex.getMessage().contains("not pending review"));
    }

    @Test
    void expirePendingItems_DeletesExpiredItemsFromStorage() {
        EvidenceItem expired1 = EvidenceItem.builder()
            .id("e1").userId("u1").status(EvidenceStatus.PENDING_REVIEW)
            .minioObjectKey("key1").sha256Hash("h").build();
        EvidenceItem expired2 = EvidenceItem.builder()
            .id("e2").userId("u2").status(EvidenceStatus.PENDING_REVIEW)
            .minioObjectKey("key2").sha256Hash("h").build();

        when(evidenceRepository.findExpiredPendingItems(anyLong()))
            .thenReturn(List.of(expired1, expired2));

        evidenceAccessService.expirePendingItems();

        assertEquals(EvidenceStatus.DELETED, expired1.getStatus());
        assertEquals(EvidenceStatus.DELETED, expired2.getStatus());
        verify(minioStorageService).delete("key1");
        verify(minioStorageService).delete("key2");
        verify(auditService, times(2)).log(any(EvidenceItem.class), eq("AUTO_EXPIRED"));
    }
}
