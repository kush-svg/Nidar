package com.example.nidar.sos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SosTriggerRequest(
    @NotBlank  String userId,
    @NotNull   Double latitude,
    @NotNull   Double longitude,
               Integer batteryLevel
) {}
