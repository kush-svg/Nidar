package com.example.nidar.auth.dto;


public record AuthResponseDto(
    String  accessToken,          // 15-min JWT
    String  refreshToken,         // 30-day JWT
    String  userId,
    String  name,
    boolean isNewUser             // true → Android shows onboarding
) {}
