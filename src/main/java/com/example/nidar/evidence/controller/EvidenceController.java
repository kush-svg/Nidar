package com.example.nidar.evidence.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.List;
import com.example.nidar.evidence.model.CaptureMode;
import com.example.nidar.evidence.model.EvidenceItem;

import com.example.nidar.evidence.dto.EvidenceItemDto;
import com.example.nidar.evidence.dto.EvidenceMetadata;
import com.example.nidar.evidence.dto.LocationSnapshotRequest;

import com.example.nidar.evidence.service.EvidenceIntakeService;
import com.example.nidar.evidence.service.EvidenceAccessService;
import com.example.nidar.evidence.service.LocationSnapshotService;

@RestController
@RequestMapping("/api/v1/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceIntakeService   intakeService;
    private final EvidenceAccessService   accessService;
    private final LocationSnapshotService snapshotService;

    // Manual upload
    @PostMapping("/upload")
    public ResponseEntity<EvidenceItemDto> upload(
        @RequestParam("file")      MultipartFile file,
        @RequestParam("userId")    String userId,
        @RequestParam("deviceId")  String deviceId,
        @RequestParam("capturedAt") Long capturedAt,
        @RequestParam(value = "sosSessionId", required = false) String sosSessionId,
        HttpServletRequest httpRequest
    ) throws IOException {
        EvidenceMetadata meta = EvidenceMetadata.builder()
            .userId(userId)
            .sosSessionId(sosSessionId)
            .captureMode(CaptureMode.MANUAL)
            .deviceId(deviceId)
            .capturedAt(capturedAt)
            .networkType(httpRequest.getHeader("X-Network-Type"))
            .build();

        EvidenceItem item = intakeService.ingest(file, meta);
        return ResponseEntity.ok(EvidenceItemDto.from(item));
    }

    // Location snapshot during SOS
    @PostMapping("/location-snapshot")
    public ResponseEntity<Void> locationSnapshot(
        @Valid @RequestBody LocationSnapshotRequest request
    ) {
        snapshotService.record(
            request.sosSessionId(), request.userId(),
            request.lat(), request.lng(), request.accuracy()
        );
        return ResponseEntity.ok().build();
    }

    // Fetch vault contents — owner only
    @GetMapping("/{userId}/items")
    public ResponseEntity<List<EvidenceItemDto>> getVault(
        @PathVariable String userId,
        @RequestHeader("X-Vault-Token") String vaultToken  // separate vault auth
    ) {
        accessService.validateVaultToken(userId, vaultToken);
        return ResponseEntity.ok(accessService.getVaultItems(userId));
    }

    // Generate pre-signed download URL — 15-minute expiry
    @GetMapping("/{itemId}/download-url")
    public ResponseEntity<String> getDownloadUrl(
        @PathVariable String itemId,
        @RequestHeader("X-Vault-Token") String vaultToken
    ) {
        return ResponseEntity.ok(accessService.generatePresignedUrl(itemId, vaultToken));
    }

    // User confirms or deletes a PENDING_REVIEW auto-capture
    @PostMapping("/{itemId}/review")
    public ResponseEntity<Void> reviewItem(
        @PathVariable String itemId,
        @RequestParam String decision,   // "KEEP" or "DELETE"
        @RequestHeader("X-Vault-Token") String vaultToken
    ) {
        accessService.processReview(itemId, decision, vaultToken);
        return ResponseEntity.ok().build();
    }
}
