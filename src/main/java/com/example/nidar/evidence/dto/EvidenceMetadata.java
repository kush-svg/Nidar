package com.example.nidar.evidence.dto;

import lombok.Builder;
import com.example.nidar.evidence.model.CaptureMode;
@Builder
public record EvidenceMetadata(
    String      userId,
    String      sosSessionId,    // null if manual upload
    CaptureMode captureMode,
    String      deviceId,
    Long        capturedAt,      // epoch seconds — device time
    Integer     batteryLevel,
    String      networkType,     // WIFI, 4G, 3G
    Double      latitude,        // where file was captured
    Double      longitude
) {}
