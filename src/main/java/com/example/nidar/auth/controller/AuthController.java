package com.example.nidar.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.example.nidar.auth.service.OtpService;
import com.example.nidar.auth.service.JwtService;
import com.example.nidar.auth.service.UserService;

import com.example.nidar.auth.dto.OtpRequestDto;
import com.example.nidar.auth.dto.OtpVerifyDto;
import com.example.nidar.auth.dto.AuthResponseDto;

import com.example.nidar.auth.model.User;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final OtpService      otpService;
    private final JwtService      jwtService;
    private final UserService     userService;

    // Step 1 — request OTP (Legacy/Reference)
    @PostMapping("/otp/request")
    public ResponseEntity<String> requestOtp(
        @Valid @RequestBody OtpRequestDto request
    ) {
        otpService.generateAndSend(request.phoneNumber());
        return ResponseEntity.ok("OTP sent");
    }


    // Step 2 (Legacy) — verify OTP, get tokens
    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponseDto> verifyOtp(
        @Valid @RequestBody OtpVerifyDto request
    ) {
        // Verify OTP — throws if invalid or expired
        otpService.verify(request.phoneNumber(), request.otp());

        // Get or create user
        boolean isNewUser = !userService.existsByPhone(request.phoneNumber());
        User user = userService.getOrCreate(request.phoneNumber());

        // Issue tokens
        String accessToken  = jwtService.issueAccessToken(
            user.getId(), user.getRole().name()
        );
        String refreshToken = jwtService.issueRefreshToken(user.getId());

        return ResponseEntity.ok(new AuthResponseDto(
            accessToken,
            refreshToken,
            user.getId(),
            user.getName(),
            isNewUser
        ));
    }

    // Step 3 — refresh access token
    @PostMapping("/token/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(
        @RequestHeader("Authorization") String authHeader
    ) {
        String refreshToken = authHeader.substring(7);

        if (!jwtService.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = jwtService.extractUserId(refreshToken);
        User   user   = userService.getById(userId);

        String newAccessToken = jwtService.issueAccessToken(
            user.getId(), user.getRole().name()
        );

        return ResponseEntity.ok(new AuthResponseDto(
            newAccessToken,
            refreshToken,   // same refresh token — not rotated
            user.getId(),
            user.getName(),
            false
        ));
    }
}
