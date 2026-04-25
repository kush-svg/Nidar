package com.example.nidar.evidence.dto;

import com.example.nidar.evidence.model.EvidenceItem;
import com.example.nidar.evidence.model.EvidenceType;
import com.example.nidar.evidence.model.CaptureMode;
import com.example.nidar.evidence.model.EvidenceStatus;

public record EvidenceItemDto(
    String         id,
    String         userId,
    String         sosSessionId,
    EvidenceType   type,
    CaptureMode    captureMode,
    EvidenceStatus status,
    Long           capturedAt,
    Long           uploadedAt,
    String         sha256Hash,
    Long           fileSizeBytes,
    Double         latitude,
    Double         longitude
    // Note: minioObjectKey is intentionally excluded — never expose storage paths
) {
    public static EvidenceItemDto from(EvidenceItem item) {
        return new EvidenceItemDto(
            item.getId(),
            item.getUserId(),
            item.getSosSessionId(),
            item.getType(),
            item.getCaptureMode(),
            item.getStatus(),
            item.getCapturedAt(),
            item.getUploadedAt(),
            item.getSha256Hash(),
            item.getFileSizeBytes(),
            item.getLatitude(),
            item.getLongitude()
        );
    }
}
