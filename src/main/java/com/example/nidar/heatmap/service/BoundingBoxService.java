package com.example.nidar.heatmap.service;

import org.springframework.stereotype.Service;
import com.example.nidar.heatmap.dto.BoundingBox;

@Service
public class BoundingBoxService {

    // Converts a center point + radius into a bounding box
    // Uses your exact document formulas
    public BoundingBox compute(double centerLat, double centerLng, double radiusKm) {
        double latOffset = radiusKm / 111.0;

        // Longitude degrees shrink near poles — cosine correction
        double lngOffset = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));

        return new BoundingBox(
            centerLat - latOffset,   // minLat
            centerLat + latOffset,   // maxLat
            centerLng - lngOffset,   // minLng
            centerLng + lngOffset    // maxLng
        );
    }

}
