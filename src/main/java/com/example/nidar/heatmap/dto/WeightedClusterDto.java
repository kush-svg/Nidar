package com.example.nidar.heatmap.dto;

// heatmap/dto/WeightedClusterDto.java
public record WeightedClusterDto(
    double lat,
    double lng,
    double dangerScore    // This feeds directly into WeightedLatLng(LatLng, dangerScore)
) {}
