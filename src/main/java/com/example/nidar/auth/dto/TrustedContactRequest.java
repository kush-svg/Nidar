package com.example.nidar.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TrustedContactRequest(
    @NotBlank String name,
    @NotBlank String phoneNumber
) {}
