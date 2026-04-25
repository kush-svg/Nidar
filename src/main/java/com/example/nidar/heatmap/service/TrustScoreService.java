package com.example.nidar.heatmap.service;

import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.UserRole;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.heatmap.repository.IncidentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final UserRepository        userRepository;
    private final IncidentRepository    incidentRepository;

    // Calculate trust score dynamically based on user history
    public double calculateTrustScore(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0.2;  // new unknown user

        // Base score from account age
        long accountAgeDays = (Instant.now().getEpochSecond()
            - user.getCreatedAt()) / 86_400;

        double baseScore = Math.min(0.5, accountAgeDays / 60.0);

        // Bonus for verified protector
        if (user.getRole() == UserRole.PROTECTOR) baseScore += 0.2;

        // Bonus for past report accuracy
        // (in production: compare past reports with police records)
        long pastReports = incidentRepository.countByUserId(userId);
        if (pastReports > 5)  baseScore += 0.1;
        if (pastReports > 20) baseScore += 0.1;

        // Cap at 1.0
        return Math.min(1.0, baseScore);
    }
}