package com.example.nidar.auth.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.example.nidar.auth.service.VaultAuthService;
import com.example.nidar.auth.dto.VaultUnlockDto;

@RestController
@RequestMapping("/auth/vault")
@RequiredArgsConstructor
public class VaultAuthController {

    private final VaultAuthService vaultAuthService;

    // First login — set up vault PIN
    @PostMapping("/setup")
    public ResponseEntity<String> setupPin(
        @RequestParam String userId,
        @RequestParam String pin
    ) {
        vaultAuthService.setupVaultPin(userId, pin);
        return ResponseEntity.ok("Vault PIN set up successfully");
    }

    // Unlock vault — PIN or biometric
    @PostMapping("/unlock")
    public ResponseEntity<String> unlock(
        @Valid @RequestBody VaultUnlockDto request
    ) {
        String vaultToken;

        if (request.pin() != null) {
            // PIN path
            vaultToken = vaultAuthService.unlockWithPin(
                request.userId(), request.pin()
            );
        } else if (request.biometricToken() != null) {
            // Biometric path
            vaultToken = vaultAuthService.unlockWithBiometric(
                request.userId(), request.biometricToken()
            );
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(vaultToken);
    }

    // Enable biometric after PIN is already set up
    @PostMapping("/biometric/enable")
    public ResponseEntity<String> enableBiometric(
        @RequestParam String userId
    ) {
        vaultAuthService.enableBiometric(userId);
        return ResponseEntity.ok("Biometric enabled for vault");
    }
}