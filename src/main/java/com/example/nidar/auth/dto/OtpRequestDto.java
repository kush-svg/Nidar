package com.example.nidar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequestDto(
    @NotBlank @Pattern(regexp = "^91[6-9]\\d{9}$",
    message = "Phone must be in format 91XXXXXXXXXX")
    String phoneNumber
) {}
