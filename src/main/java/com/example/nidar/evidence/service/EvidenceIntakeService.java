package com.example.nidar.evidence.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.HexFormat;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.security.InvalidKeyException;

import javax.crypto.Cipher;

import com.example.nidar.evidence.model.EvidenceItem;
import com.example.nidar.evidence.model.EvidenceType;
import com.example.nidar.evidence.model.EvidenceStatus;
import com.example.nidar.evidence.model.CaptureMode;

import com.example.nidar.evidence.dto.EvidenceMetadata;

import com.example.nidar.evidence.repository.EvidenceRepository;
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceIntakeService {

    private final MinioStorageService  minioStorageService;
    private final HashChainService     hashChainService;
    private final EvidenceAuditService auditService;
    private final EvidenceRepository   evidenceRepository;
    private final KeyVaultService      keyVaultService;
    private static final int GCM_IV_LENGTH  = 12;   // 96 bits — GCM standard
    private static final int GCM_TAG_LENGTH = 128;  // authentication tag bits

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "audio/mpeg", "audio/mp4", "audio/aac",
        "image/jpeg", "image/png",
        "application/pdf"
    );
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100MB

    public EvidenceItem ingest(
        MultipartFile    file,
        EvidenceMetadata meta   // device ID, battery, location, SOS session
    ) throws IOException {

        // ── 1. Validate ──────────────────────────────────────────────────────
        validateFile(file);

        // ── 2. Compute SHA-256 BEFORE encryption ─────────────────────────────
        byte[] fileBytes = file.getBytes();
        String sha256    = computeSha256(fileBytes);

        // ── 3. Encrypt with AES-256-GCM ──────────────────────────────────────
        byte[] encrypted = encryptAes256Gcm(fileBytes, meta.userId());

        // ── 4. Upload encrypted bytes to MinIO ─────────────────────────────────
        String objectKey = minioStorageService.upload(
            meta.userId(), encrypted, file.getContentType()
        );

        // ── 5. Build the hash chain link ─────────────────────────────────────
        String previousHash = hashChainService.getLatestHash(meta.userId());
        String chainHash    = hashChainService.computeChainHash(previousHash, sha256);

        // ── 6. Persist evidence record ───────────────────────────────────────
        EvidenceItem item = EvidenceItem.builder()
            .id(UUID.randomUUID().toString())
            .userId(meta.userId())
            .sosSessionId(meta.sosSessionId())
            .type(resolveType(file.getContentType()))
            .captureMode(meta.captureMode())
            .minioObjectKey(objectKey)
            .fileSizeBytes((long) fileBytes.length)
            .sha256Hash(sha256)
            .previousHash(previousHash)
            .chainHash(chainHash)
            .capturedAt(meta.capturedAt())
            .uploadedAt(Instant.now().getEpochSecond())
            .deviceId(meta.deviceId())
            .batteryLevel(meta.batteryLevel())
            .networkType(meta.networkType())
            .latitude(meta.latitude())
            .longitude(meta.longitude())
            .status(meta.captureMode() == CaptureMode.AUTO
                    ? EvidenceStatus.PENDING_REVIEW   // auto → needs confirmation
                    : EvidenceStatus.CONFIRMED)       // manual → immediately confirmed
            .build();

        evidenceRepository.save(item);

        // ── 7. Append to audit log ────────────────────────────────────────────
        auditService.log(item, "UPLOADED");

        return item;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("Empty file rejected");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException("File exceeds 100MB limit");

        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("File type not allowed: "
                + file.getContentType());
    }

    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private byte[] encryptAes256Gcm(byte[] plaintext, String userId) {
    try {
        SecretKey key = keyVaultService.getOrCreateKey(userId);

        // Generate a random IV — unique per encryption, never reuse
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] encryptedData = cipher.doFinal(plaintext);

        // Prepend IV to the ciphertext — IV is needed for decryption
        // Format stored in GCS: [12 bytes IV][encrypted data + 16 byte auth tag]
        byte[] result = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv,            0, result, 0,              GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH,  encryptedData.length);

        return result;

    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
        throw new RuntimeException("AES/GCM not available", e);
    } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
        throw new RuntimeException("Invalid encryption key or IV", e);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
        throw new RuntimeException("Encryption failed", e);
    }
}

    private EvidenceType resolveType(String contentType) {
        if (contentType.startsWith("audio/"))       return EvidenceType.AUDIO;
        if (contentType.startsWith("image/"))       return EvidenceType.PHOTO;
        if (contentType.equals("application/pdf"))  return EvidenceType.DOCUMENT;
        return EvidenceType.DOCUMENT;
    }
}