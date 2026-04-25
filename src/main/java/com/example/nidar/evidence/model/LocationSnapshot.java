package com.example.nidar.evidence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "location_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSnapshot {

    @Id
    private String id;

    private String sosSessionId;
    private String userId;

    private Double  latitude;
    private Double  longitude;
    private Float   accuracy;
    private Long    capturedAt;     // epoch seconds

    private String  sha256Hash;     // hash of this snapshot
    private String  chainHash;      // hash(prevHash + sha256Hash)
    private String  previousHash;
}
