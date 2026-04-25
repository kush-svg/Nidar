package com.example.nidar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpVerifyDto(
    @NotBlank String phoneNumber,
    @NotBlank @Size(min = 6, max = 6) String otp
) {}
