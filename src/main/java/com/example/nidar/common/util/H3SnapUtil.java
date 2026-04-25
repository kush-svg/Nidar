package com.example.nidar.common.util;

import org.springframework.stereotype.Component;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;

import java.io.IOException;
import java.util.List;

@Component
public class H3SnapUtil {

    private final H3Core h3;

    // Resolution 7 = hexagons ~1.2km² area, ~600m edge-to-edge
    // Resolution 8 = ~0.7km² — use for dense cities like Delhi/Mumbai
    // See: https://h3geo.org/docs/core-library/restable/
    private static final int H3_RESOLUTION = 7;

    public H3SnapUtil() throws IOException {
        this.h3 = H3Core.newInstance();
    }

    // Get the H3 cell index for a coordinate
    public String cellIndex(double lat, double lng) {
        return h3.latLngToCellAddress(lat, lng, H3_RESOLUTION);
    }

    // Snap coordinate to the CENTER of its H3 hexagon
    // This is the privacy guarantee — exact location becomes cell centroid
    public double[] snapToCell(double lat, double lng) {
        String cellAddress = cellIndex(lat, lng);
        LatLng center = h3.cellToLatLng(cellAddress);
        return new double[]{ center.lat, center.lng };
    }

    // Get all neighboring cells — useful for bounding box queries
    // k=1 gives the 6 immediate neighbors, k=2 gives the ring beyond that
    public List<String> getNeighborCells(String cellAddress, int k) {
        return h3.gridDisk(cellAddress, k);
    }

    // Inverse of cellIndex: given an H3 cell address, return its centroid as [lat, lng]
    public double[] cellToCenter(String cellAddress) {
        LatLng center = h3.cellToLatLng(cellAddress);
        return new double[]{ center.lat, center.lng };
    }
}
