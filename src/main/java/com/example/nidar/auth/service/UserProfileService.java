package com.example.nidar.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.UserRole;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.auth.dto.UpdateProfileRequest;
import com.example.nidar.auth.dto.UserProfileResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(
        String userId,
        UpdateProfileRequest request
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.fcmToken() != null) {
            user.setFcmToken(request.fcmToken());
        }
        if (request.role() != null) {
            user.setRole(UserRole.valueOf(request.role()));
        }
        if (request.h3Index() != null) {
            user.setH3Index(request.h3Index());
        }

        user.setLastSeenAt(Instant.now().getEpochSecond());
        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deactivateAccount(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("Account deactivated for user: {}", userId);
    }
}
