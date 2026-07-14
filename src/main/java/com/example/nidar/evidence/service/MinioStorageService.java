package com.example.nidar.evidence.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.presigned-url-expiry-minutes:15}")
    private int presignedUrlExpiryMinutes;

    public String upload(String userId, byte[] data, String contentType) {
        String objectKey = String.format("%s/%s/%s", 
            userId, YearMonth.now().toString(), UUID.randomUUID().toString());
            
        try (InputStream is = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(is, data.length, -1)
                    .contentType(contentType)
                    .build()
            );
            log.info("File uploaded to MinIO: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("MinIO upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to storage", e);
        }
    }

    public String generatePresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(presignedUrlExpiryMinutes, TimeUnit.MINUTES)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate MinIO presigned URL: {}", e.getMessage(), e);
            throw new RuntimeException("Could not generate download link", e);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            log.info("File deleted from MinIO: {}", objectKey);
        } catch (Exception e) {
            log.error("MinIO delete failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from storage", e);
        }
    }
}
