package com.example.nidar.heatmap.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import org.locationtech.jts.geom.Point;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "incidents", indexes = {
    @Index(name = "idx_h3_index", columnList = "h3_index"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_lat_lng", columnList = "latitude, longitude")
})
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // WHERE
    private Double latitude;
    private Double longitude;
    @Column(name = "h3_index")
    private String h3Index;          // e.g. "ttnfv2" — pre-snapped to 500m grid

    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point  geom;             // PostGIS geometry — set alongside lat/lng

    // WHEN
    private Long timestamp;          // Unix epoch seconds

    // WHAT
    @Enumerated(EnumType.STRING)
    private IncidentType incidentType;
    private Integer baseWeight;

    // WHO
    private String userId;
    private Double trustScore;       // 0.0 to 1.0
}