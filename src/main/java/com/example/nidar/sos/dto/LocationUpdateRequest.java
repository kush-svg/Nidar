package com.example.nidar.sos.dto;

import jakarta.validation.constraints.NotNull;

// sos/dto/LocationUpdateRequest.java
public record LocationUpdateRequest(
    @NotNull Double  lat,
    @NotNull Double  lng,
             Float   accuracy,
             Integer batteryLevel
) {}
