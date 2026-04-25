package com.example.nidar.auth.dto;

public record UpdateProfileRequest(
    String  name,
    String  fcmToken,
    String  role,
    String  h3Index
) {}
