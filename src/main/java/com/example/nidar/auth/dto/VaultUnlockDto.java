package com.example.nidar.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VaultUnlockDto(
    @NotBlank String userId,
    String pin,                   // null if using biometric
    String biometricToken         // null if using PIN — Android biometric challenge result
) {}
