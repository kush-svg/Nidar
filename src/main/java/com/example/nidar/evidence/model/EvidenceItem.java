package com.example.nidar.evidence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "evidence_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceItem {

    @Id
    private String id;                  // UUID

    private String userId;              // owner — never changeable after creation
    private String sosSessionId;        // null if manually uploaded

    @Enumerated(EnumType.STRING)
    private EvidenceType type;          // AUDIO, PHOTO, DOCUMENT, LOCATION_LOG

    @Enumerated(EnumType.STRING)
    private CaptureMode captureMode;    // AUTO (SOS) or MANUAL

    // Storage
    private String minioObjectKey;      // path inside GCS bucket
    private Long   fileSizeBytes;

    // Integrity
    @Column(name = "sha256_hash")
    private String sha256Hash;          // computed before encryption

    @Column(name = "previous_hash")
    private String previousHash;        // hash of previous item — chain link

    @Column(name = "chain_hash")
    private String chainHash;           // hash(previousHash + sha256Hash)

    // Metadata — the Section 65B data
    private Long    capturedAt;         // epoch seconds — device time
    private Long    uploadedAt;         // epoch seconds — server time
    private String  deviceId;
    private Integer batteryLevel;
    private String  networkType;        // WIFI, 4G, 3G
    private Double  latitude;           // where it was captured
    private Double  longitude;

    @Enumerated(EnumType.STRING)
    private EvidenceStatus status;      // PENDING_REVIEW, CONFIRMED, DELETED
}
