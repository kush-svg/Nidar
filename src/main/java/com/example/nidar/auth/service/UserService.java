package com.example.nidar.auth.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.UserRole;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean existsByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).isPresent();
    }

    public User getOrCreate(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .id(UUID.randomUUID().toString())
                    .phoneNumber(phoneNumber)
                    .role(UserRole.USER)
                    .isActive(true)
                    .createdAt(Instant.now().getEpochSecond())
                    .lastSeenAt(Instant.now().getEpochSecond())
                    .build()
            ));
    }

    public User getById(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    // Called on every API request — keeps lastSeenAt fresh for protector queries
    public void updateLastSeen(String userId, String h3Index) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(Instant.now().getEpochSecond());
            if (h3Index != null) user.setH3Index(h3Index);
            userRepository.save(user);
        });
    }
}
