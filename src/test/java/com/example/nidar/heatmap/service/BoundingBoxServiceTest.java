package com.example.nidar.heatmap.service;

import org.junit.jupiter.api.Test;

import com.example.nidar.heatmap.dto.BoundingBox;

import static org.junit.jupiter.api.Assertions.*;

class BoundingBoxServiceTest {

    private final BoundingBoxService boundingBoxService = new BoundingBoxService();

    @Test
    void compute_ReturnsSymmetricBoundingBox() {
        // Delhi center: 28.6139, 77.2090
        BoundingBox bbox = boundingBoxService.compute(28.6139, 77.2090, 5.0);

        // latOffset = 5.0 / 111.0 ≈ 0.045
        double expectedLatOffset = 5.0 / 111.0;

        assertEquals(28.6139 - expectedLatOffset, bbox.minLat(), 0.001);
        assertEquals(28.6139 + expectedLatOffset, bbox.maxLat(), 0.001);
    }

    @Test
    void compute_LongitudeOffsetIsCosineAdjusted() {
        BoundingBox bbox = boundingBoxService.compute(28.6139, 77.2090, 5.0);

        double expectedLngOffset = 5.0 / (111.0 * Math.cos(Math.toRadians(28.6139)));

        assertEquals(77.2090 - expectedLngOffset, bbox.minLng(), 0.001);
        assertEquals(77.2090 + expectedLngOffset, bbox.maxLng(), 0.001);
    }

    @Test
    void compute_LargerRadius_ProducesLargerBox() {
        BoundingBox small = boundingBoxService.compute(28.6139, 77.2090, 1.0);
        BoundingBox large = boundingBoxService.compute(28.6139, 77.2090, 10.0);

        assertTrue(large.maxLat() - large.minLat() > small.maxLat() - small.minLat());
        assertTrue(large.maxLng() - large.minLng() > small.maxLng() - small.minLng());
    }

    @Test
    void compute_ZeroRadius_ReturnsCenterPoint() {
        BoundingBox bbox = boundingBoxService.compute(28.6139, 77.2090, 0.0);

        assertEquals(28.6139, bbox.minLat(), 0.001);
        assertEquals(28.6139, bbox.maxLat(), 0.001);
        assertEquals(77.2090, bbox.minLng(), 0.001);
        assertEquals(77.2090, bbox.maxLng(), 0.001);
    }

    @Test
    void compute_AtEquator_LatAndLngOffsetsAreEqual() {
        // At equator (lat=0), cos(0) = 1, so lat and lng offsets should be identical
        BoundingBox bbox = boundingBoxService.compute(0.0, 77.0, 5.0);

        double latRange = bbox.maxLat() - bbox.minLat();
        double lngRange = bbox.maxLng() - bbox.minLng();

        assertEquals(latRange, lngRange, 0.001);
    }

    @Test
    void compute_NearPoles_LongitudeOffsetIsLarger() {
        // At high latitude, longitude offset should be significantly larger
        BoundingBox equator = boundingBoxService.compute(0.0, 77.0, 5.0);
        BoundingBox highLat = boundingBoxService.compute(60.0, 77.0, 5.0);

        double equatorLngRange = equator.maxLng() - equator.minLng();
        double highLatLngRange = highLat.maxLng() - highLat.minLng();

        assertTrue(highLatLngRange > equatorLngRange);
    }
}
