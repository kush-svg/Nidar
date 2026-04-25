package com.example.nidar.home.dto;

import java.util.List;

public record HomeSummaryDto(
    int                      riskScore,
    String                   riskLevel,        // SAFE, CAUTION, DANGER
    String                   areaName,
    int                      protectorCount,
    List<NearbyProtectorDto> nearbyProtectors
) {}
