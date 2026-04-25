package com.example.nidar.auth.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;

import com.example.nidar.auth.service.UserProfileService;
import com.example.nidar.auth.dto.UpdateProfileRequest;
import com.example.nidar.auth.dto.UserProfileResponse;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
        @RequestBody UpdateProfileRequest request
    ) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(
            profileService.updateProfile(userId, request)
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivateAccount() {
        String userId = getCurrentUserId();
        profileService.deactivateAccount(userId);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext()
            .getAuthentication().getName();
    }
}
