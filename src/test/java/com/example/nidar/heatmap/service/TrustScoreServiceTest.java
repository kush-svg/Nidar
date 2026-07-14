package com.example.nidar.heatmap.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.UserRole;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.heatmap.repository.IncidentRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private TrustScoreService trustScoreService;

    @Test
    void calculateTrustScore_UnknownUser_Returns0_2() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        double score = trustScoreService.calculateTrustScore("unknown");

        assertEquals(0.2, score, 0.001);
    }

    @Test
    void calculateTrustScore_NewUser_HasLowScore() {
        User newUser = User.builder()
            .id("u1").role(UserRole.USER)
            .createdAt(Instant.now().getEpochSecond()) // just created
            .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(newUser));
        when(incidentRepository.countByUserId("u1")).thenReturn(0L);

        double score = trustScoreService.calculateTrustScore("u1");

        assertTrue(score < 0.1); // very new user should have near-zero base
    }

    @Test
    void calculateTrustScore_ProtectorRole_Gets0_2Bonus() {
        long thirtyDaysAgo = Instant.now().getEpochSecond() - (30L * 86_400);
        User protector = User.builder()
            .id("u1").role(UserRole.PROTECTOR)
            .createdAt(thirtyDaysAgo)
            .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(protector));
        when(incidentRepository.countByUserId("u1")).thenReturn(0L);

        double scoreProtector = trustScoreService.calculateTrustScore("u1");

        // Same user as USER role (no protector bonus)
        User normalUser = User.builder()
            .id("u2").role(UserRole.USER)
            .createdAt(thirtyDaysAgo)
            .build();
        when(userRepository.findById("u2")).thenReturn(Optional.of(normalUser));
        when(incidentRepository.countByUserId("u2")).thenReturn(0L);

        double scoreNormal = trustScoreService.calculateTrustScore("u2");

        assertEquals(0.2, scoreProtector - scoreNormal, 0.01);
    }

    @Test
    void calculateTrustScore_UserWithManyReports_GetsBonus() {
        long sixtyDaysAgo = Instant.now().getEpochSecond() - (60L * 86_400);
        User user = User.builder()
            .id("u1").role(UserRole.USER)
            .createdAt(sixtyDaysAgo)
            .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(incidentRepository.countByUserId("u1")).thenReturn(25L);

        double score = trustScoreService.calculateTrustScore("u1");

        // 60 days = base 0.5, +0.1 for >5 reports, +0.1 for >20 reports = 0.7
        assertEquals(0.7, score, 0.05);
    }

    @Test
    void calculateTrustScore_NeverExceeds1_0() {
        long veryOld = Instant.now().getEpochSecond() - (365L * 86_400);
        User veteranProtector = User.builder()
            .id("u1").role(UserRole.PROTECTOR)
            .createdAt(veryOld)
            .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(veteranProtector));
        when(incidentRepository.countByUserId("u1")).thenReturn(100L);

        double score = trustScoreService.calculateTrustScore("u1");

        assertTrue(score <= 1.0);
    }
}
