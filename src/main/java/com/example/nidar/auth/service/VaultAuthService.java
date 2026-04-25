package com.example.nidar.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import com.example.nidar.auth.repository.VaultCredentialRepository;
import com.example.nidar.auth.model.VaultCredential;

@Service
@RequiredArgsConstructor
@Slf4j
public class VaultAuthService {

    private final VaultCredentialRepository vaultCredentialRepository;
    private final JwtService                jwtService;
    private final PasswordEncoder           passwordEncoder;

    // Called on first login — user sets their vault PIN
    public void setupVaultPin(String userId, String rawPin) {
        if (rawPin == null || rawPin.length() != 6 || !rawPin.matches("\\d{6}")) {
            throw new IllegalArgumentException("PIN must be exactly 6 digits");
        }

        VaultCredential credential = VaultCredential.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .hashedPin(passwordEncoder.encode(rawPin))
            .biometricEnabled(false)
            .createdAt(Instant.now().getEpochSecond())
            .updatedAt(Instant.now().getEpochSecond())
            .build();

        vaultCredentialRepository.save(credential);
    }

    // Called when user opens vault — PIN path
    public String unlockWithPin(String userId, String rawPin) {
        VaultCredential credential = vaultCredentialRepository
            .findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Vault not set up for user: " + userId));

        if (!passwordEncoder.matches(rawPin, credential.getHashedPin())) {
            throw new RuntimeException("Invalid vault PIN");
        }

        return jwtService.issueVaultToken(userId);
    }

    // Called when user opens vault — biometric path
    // Android passes a biometric challenge token — we verify it was issued for this user
    public String unlockWithBiometric(String userId, String biometricToken) {
        // Biometric token is a short-lived JWT issued by Android's BiometricPrompt
        // We verify it's valid and belongs to this user
        if (!jwtService.validateToken(biometricToken)) {
            throw new RuntimeException("Invalid biometric token");
        }

        String tokenUserId = jwtService.extractUserId(biometricToken);
        if (!tokenUserId.equals(userId)) {
            throw new RuntimeException("Biometric token does not match user");
        }

        return jwtService.issueVaultToken(userId);
    }

    public void enableBiometric(String userId) {
        VaultCredential credential = vaultCredentialRepository
            .findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Vault not set up for user: " + userId));

        credential.setBiometricEnabled(true);
        credential.setUpdatedAt(Instant.now().getEpochSecond());
        vaultCredentialRepository.save(credential);
    }
}
