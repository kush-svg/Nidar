package com.example.nidar.home.dto;

public record NearbyProtectorDto(
    String name,
    double distanceKm,
    String h3Index
) {}
