package com.example.nidar.heatmap.dto;

public record BoundingBox(
        double minLat,
        double maxLat,
        double minLng,
        double maxLng
) {}