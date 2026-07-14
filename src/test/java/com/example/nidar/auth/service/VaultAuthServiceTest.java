package com.example.nidar.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.nidar.auth.model.VaultCredential;
import com.example.nidar.auth.repository.VaultCredentialRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultAuthServiceTest {

    @Mock
    private VaultCredentialRepository vaultCredentialRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private VaultAuthService vaultAuthService;

    @Test
    void setupVaultPin_WithValid6DigitPin_SavesCredential() {
        when(passwordEncoder.encode("123456")).thenReturn("hashed_123456");

        vaultAuthService.setupVaultPin("u1", "123456");

        verify(vaultCredentialRepository).save(any(VaultCredential.class));
    }

    @Test
    void setupVaultPin_WithNullPin_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> vaultAuthService.setupVaultPin("u1", null));
    }

    @Test
    void setupVaultPin_WithShortPin_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> vaultAuthService.setupVaultPin("u1", "123"));
    }

    @Test
    void setupVaultPin_WithAlphanumericPin_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> vaultAuthService.setupVaultPin("u1", "abc123"));
    }

    @Test
    void setupVaultPin_WithLongPin_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> vaultAuthService.setupVaultPin("u1", "1234567"));
    }

    @Test
    void unlockWithPin_CorrectPin_ReturnsVaultToken() {
        VaultCredential cred = VaultCredential.builder()
            .userId("u1").hashedPin("hashed_pin").build();

        when(vaultCredentialRepository.findByUserId("u1")).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("123456", "hashed_pin")).thenReturn(true);
        when(jwtService.issueVaultToken("u1")).thenReturn("vault-jwt-token");

        String token = vaultAuthService.unlockWithPin("u1", "123456");

        assertEquals("vault-jwt-token", token);
    }

    @Test
    void unlockWithPin_IncorrectPin_ThrowsException() {
        VaultCredential cred = VaultCredential.builder()
            .userId("u1").hashedPin("hashed_pin").build();

        when(vaultCredentialRepository.findByUserId("u1")).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("000000", "hashed_pin")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vaultAuthService.unlockWithPin("u1", "000000"));
        assertTrue(ex.getMessage().contains("Invalid vault PIN"));
    }

    @Test
    void unlockWithPin_VaultNotSetUp_ThrowsException() {
        when(vaultCredentialRepository.findByUserId("u1")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vaultAuthService.unlockWithPin("u1", "123456"));
        assertTrue(ex.getMessage().contains("Vault not set up"));
    }

    @Test
    void unlockWithBiometric_ValidToken_ReturnsVaultToken() {
        when(jwtService.validateToken("bio-token")).thenReturn(true);
        when(jwtService.extractUserId("bio-token")).thenReturn("u1");
        when(jwtService.issueVaultToken("u1")).thenReturn("vault-jwt-token");

        String token = vaultAuthService.unlockWithBiometric("u1", "bio-token");

        assertEquals("vault-jwt-token", token);
    }

    @Test
    void unlockWithBiometric_InvalidToken_ThrowsException() {
        when(jwtService.validateToken("bad-token")).thenReturn(false);

        assertThrows(RuntimeException.class,
            () -> vaultAuthService.unlockWithBiometric("u1", "bad-token"));
    }

    @Test
    void unlockWithBiometric_TokenBelongsToDifferentUser_ThrowsException() {
        when(jwtService.validateToken("other-user-token")).thenReturn(true);
        when(jwtService.extractUserId("other-user-token")).thenReturn("other-user");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vaultAuthService.unlockWithBiometric("u1", "other-user-token"));
        assertTrue(ex.getMessage().contains("does not match"));
    }

    @Test
    void enableBiometric_WhenVaultExists_SetsBiometricTrue() {
        VaultCredential cred = VaultCredential.builder()
            .userId("u1").biometricEnabled(false).build();

        when(vaultCredentialRepository.findByUserId("u1")).thenReturn(Optional.of(cred));

        vaultAuthService.enableBiometric("u1");

        assertTrue(cred.getBiometricEnabled());
        verify(vaultCredentialRepository).save(cred);
    }

    @Test
    void enableBiometric_WhenVaultNotSetUp_ThrowsException() {
        when(vaultCredentialRepository.findByUserId("u1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> vaultAuthService.enableBiometric("u1"));
    }
}
