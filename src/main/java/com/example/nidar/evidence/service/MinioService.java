package com.example.nidar.evidence.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class MinioService {

    @Value("${minio.bucket-name:mock-bucket}")
    private String bucketName;

    @Value("${minio.presigned-url-expiry-minutes:15}")
    private int presignedUrlExpiryMinutes;

    // ✅ MOCK upload
    public String upload(String userId, byte[] encryptedBytes, String contentType) {
        String objectKey = "%s/%s/%s".formatted(
            userId,
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")),
            UUID.randomUUID()
        );

        log.info("📦 [MOCK] Upload to bucket={} key={} size={} bytes",
                bucketName, objectKey, encryptedBytes.length);

        return objectKey;
    }

    // ✅ MOCK URL
    public String generatePresignedUrl(String objectKey) {
        String url = "http://localhost/mock/" + objectKey;

        log.info("🔗 [MOCK] Presigned URL for {} -> {}", objectKey, url);

        return url;
    }

    // ✅ MOCK delete
    public void delete(String objectKey) {
        log.info("🗑️ [MOCK] Delete object {}", objectKey);
    }
}