package com.example.nidar.evidence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.time.Instant;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyVaultService {

    // HashiCorp Vault template — configured in VaultConfig.java
    private final VaultTemplate vaultTemplate;

    private static final String KEY_PATH_PREFIX = "secret/nidar/evidence/keys/";
    private static final int    AES_KEY_SIZE    = 256;

    // Get existing key or create a new one for this user
    // One key per user — all their files use the same key
    public SecretKey getOrCreateKey(String userId) {
        String path = KEY_PATH_PREFIX + userId;

        try {
            // Try to fetch existing key from Vault
            VaultResponse response = vaultTemplate.read(path);

            if (response != null && response.getData() != null) {
                String base64Key = (String) response.getData().get("aes_key");
                byte[] keyBytes  = Base64.getDecoder().decode(base64Key);
                return new SecretKeySpec(keyBytes, "AES");
            }

            // Key doesn't exist yet — generate and store it
            return generateAndStoreKey(userId, path);

        } catch (Exception e) {
            log.error("Vault key fetch failed for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Could not retrieve encryption key", e);
        }
    }

    private SecretKey generateAndStoreKey(String userId, String path) {
        try {
            // Generate a new AES-256 key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE, new SecureRandom());
            SecretKey key = keyGen.generateKey();

            // Store in Vault — never in PostgreSQL, never in MinIO
            String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
            vaultTemplate.write(path, Map.of(
                "aes_key",    base64Key,
                "created_at", Instant.now().getEpochSecond(),
                "user_id",    userId
            ));

            log.info("New encryption key generated and stored for user: {}", userId);
            return key;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES key generation failed", e);
        }
    }

    // Called if user requests key rotation
    // Old files become unreadable — only use in extreme cases
    public void rotateKey(String userId) {
        String path    = KEY_PATH_PREFIX + userId;
        String oldPath = KEY_PATH_PREFIX + userId + "/archived/"
                         + Instant.now().getEpochSecond();

        try {
            // Archive the old key before overwriting
            VaultResponse existing = vaultTemplate.read(path);
            if (existing != null) {
                vaultTemplate.write(oldPath, existing.getData());
            }

            generateAndStoreKey(userId, path);
            log.info("Key rotated for user: {}", userId);

        } catch (Exception e) {
            log.error("Key rotation failed for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Key rotation failed", e);
        }
    }
}