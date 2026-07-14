package com.example.nidar.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class H3SnapUtilTest {

    private final H3SnapUtil h3SnapUtil;

    H3SnapUtilTest() throws Exception {
        h3SnapUtil = new H3SnapUtil();
    }

    @Test
    void cellIndex_ReturnNonNullForValidCoordinates() {
        String index = h3SnapUtil.cellIndex(28.6139, 77.2090);

        assertNotNull(index);
        assertFalse(index.isEmpty());
    }

    @Test
    void cellIndex_SameCoordinates_ReturnSameIndex() {
        String index1 = h3SnapUtil.cellIndex(28.6139, 77.2090);
        String index2 = h3SnapUtil.cellIndex(28.6139, 77.2090);

        assertEquals(index1, index2);
    }

    @Test
    void cellIndex_NearbyCoordinates_ReturnSameCellAtResolution7() {
        // Two points very close together (within 1 H3 cell at resolution 7 ~600m)
        String index1 = h3SnapUtil.cellIndex(28.6139, 77.2090);
        String index2 = h3SnapUtil.cellIndex(28.6141, 77.2091);

        assertEquals(index1, index2);
    }

    @Test
    void cellIndex_DistantCoordinates_ReturnDifferentCells() {
        String delhi  = h3SnapUtil.cellIndex(28.6139, 77.2090);
        String mumbai = h3SnapUtil.cellIndex(19.0760, 72.8777);

        assertNotEquals(delhi, mumbai);
    }

    @Test
    void snapToCell_ReturnsCoordinates() {
        double[] snapped = h3SnapUtil.snapToCell(28.6139, 77.2090);

        assertEquals(2, snapped.length);
        // Snapped coordinates should be close to input but not identical
        // H3 resolution 7 cells are ~600m across, so tolerance needs to be ~0.02°
        assertEquals(28.6139, snapped[0], 0.02);
        assertEquals(77.2090, snapped[1], 0.02);
    }

    @Test
    void snapToCell_SameCell_ReturnsSameCenter() {
        double[] snap1 = h3SnapUtil.snapToCell(28.6139, 77.2090);
        double[] snap2 = h3SnapUtil.snapToCell(28.6141, 77.2091);

        // Both should snap to the same cell center
        assertEquals(snap1[0], snap2[0], 0.0001);
        assertEquals(snap1[1], snap2[1], 0.0001);
    }

    @Test
    void getNeighborCells_K1_Returns7Cells() {
        String cellIndex = h3SnapUtil.cellIndex(28.6139, 77.2090);

        List<String> neighbors = h3SnapUtil.getNeighborCells(cellIndex, 1);

        // k=1 gridDisk returns origin + 6 neighbors = 7
        assertEquals(7, neighbors.size());
        assertTrue(neighbors.contains(cellIndex)); // includes origin
    }

    @Test
    void getNeighborCells_K0_Returns1Cell() {
        String cellIndex = h3SnapUtil.cellIndex(28.6139, 77.2090);

        List<String> neighbors = h3SnapUtil.getNeighborCells(cellIndex, 0);

        assertEquals(1, neighbors.size());
        assertEquals(cellIndex, neighbors.get(0));
    }

    @Test
    void cellToCenter_ReturnsValidCoordinates() {
        String cellIndex = h3SnapUtil.cellIndex(28.6139, 77.2090);

        double[] center = h3SnapUtil.cellToCenter(cellIndex);

        assertEquals(2, center.length);
        assertTrue(center[0] >= -90 && center[0] <= 90);  // valid latitude
        assertTrue(center[1] >= -180 && center[1] <= 180); // valid longitude
    }

    @Test
    void cellToCenter_MatchesSnapToCell() {
        double[] snapped = h3SnapUtil.snapToCell(28.6139, 77.2090);
        String cellIndex = h3SnapUtil.cellIndex(28.6139, 77.2090);
        double[] center  = h3SnapUtil.cellToCenter(cellIndex);

        assertEquals(snapped[0], center[0], 0.0001);
        assertEquals(snapped[1], center[1], 0.0001);
    }
}
