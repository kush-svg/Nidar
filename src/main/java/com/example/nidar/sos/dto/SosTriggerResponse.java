package com.example.nidar.sos.dto;


public record SosTriggerResponse(
    String sosSessionId,   // Android uses this to stream live location updates
    String status,          // "ACTIVE"
    boolean startAudioCapture,
    int locationSnapshotIntervalSeconds
) {}
