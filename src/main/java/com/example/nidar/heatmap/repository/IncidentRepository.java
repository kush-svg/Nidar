package com.example.nidar.heatmap.repository;

import com.example.nidar.heatmap.model.Incident;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {

    // ── Legacy lat/lng bounding box — used by HeatmapController ──────────────
    @Query("""
        SELECT i FROM Incident i
        WHERE i.latitude  BETWEEN :minLat AND :maxLat
          AND i.longitude BETWEEN :minLng AND :maxLng
          AND i.timestamp > :cutoffTimestamp
        """)
    List<Incident> findWithinBoundingBox(
        @Param("minLat")           double minLat,
        @Param("maxLat")           double maxLat,
        @Param("minLng")           double minLng,
        @Param("maxLng")           double maxLng,
        @Param("cutoffTimestamp")  long   cutoffTimestamp
    );

    // ── H3 cell list — used by HomeService to score the user's area ───────────
    @Query("""
        SELECT i FROM Incident i
        WHERE i.h3Index IN :cells
          AND i.timestamp > :cutoff
        """)
    List<Incident> findByH3IndexInAndTimestampAfter(
        @Param("cells")  List<String> cells,
        @Param("cutoff") long         cutoff
    );

    // ── PostGIS viewport query — ST_Within for precise map bounds ─────────────
    @Query(value = """
    SELECT * FROM incidents
    WHERE ST_Within(
        geom,
        ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
    )
    AND timestamp > :cutoff
    """, nativeQuery = true)
    List<Incident> findWithinViewport(
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLng") double minLng,
        @Param("maxLng") double maxLng,
        @Param("cutoff") long   cutoff
    );

    // ── ST_ClusterKMeans — server-side clustering ─────────────────────────────
    @Query(value = """
    SELECT
        ST_Y(ST_Centroid(ST_Collect(geom))) AS lat,
        ST_X(ST_Centroid(ST_Collect(geom))) AS lng,
        SUM(base_weight)                    AS totalWeight,
        COUNT(*)                            AS incidentCount
    FROM (
        SELECT geom, base_weight,
               ST_ClusterKMeans(geom, 20) OVER () AS cluster_id
        FROM incidents
        WHERE ST_Within(geom,
            ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
        AND timestamp > :cutoff
    ) clustered
    GROUP BY cluster_id
    """, nativeQuery = true)
    List<Object[]> clusterWithinViewport(
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLng") double minLng,
        @Param("maxLng") double maxLng,
        @Param("cutoff") long   cutoff
    );

    boolean existsByUserIdAndH3IndexAndTimestampAfter(
        String userId,
        String h3Index,
        long   cutoffTimestamp
    );

    long countByUserId(String userId);

    void deleteByUserId(String userId);
}
