package com.example.nidar.sos.model;

import lombok.Builder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.persistence.Index;
import lombok.NoArgsConstructor;

// sos/model/SosSession.java
@Entity
@Table(
    name = "active_sos",
    indexes = @Index(name = "idx_h3", columnList = "h3_index")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosSession {

    @Id
    private String sessionId;

    private String  userId;
    private Double  latitude;
    private Double  longitude;
    @Column(name = "h3_index", nullable = false)
    private String  h3Index;
    private Integer batteryLevel;

    @Enumerated(EnumType.STRING)
    private SosStatus status;

    private Long triggeredAt;
    private Long resolvedAt;
}