package com.example.nidar.evidence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationSnapshotRequest(
    @NotBlank String  sosSessionId,
    @NotBlank String  userId,
    @NotNull  Double  lat,
    @NotNull  Double  lng,
              Float   accuracy
) {}
