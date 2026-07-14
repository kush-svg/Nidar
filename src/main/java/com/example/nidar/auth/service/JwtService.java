package com.example.nidar.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.vault-secret}")
    private String vaultSecret;

    private static final long ACCESS_TOKEN_EXPIRY  = 15 * 60L;              // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY = 30 * 24 * 60 * 60L;     // 30 days
    private static final long VAULT_TOKEN_EXPIRY   = 5 * 60L;               // 5 minutes

    @jakarta.annotation.PostConstruct
    public void init() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtSecret);
        
        // Setup Encoder
        com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> jwks = 
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(keyBytes);
        this.encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(jwks);
        
        // Setup Decoder
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
        this.decoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    public String issueAccessToken(String userId, String role) {
        return buildToken(userId, Map.of("role", role), ACCESS_TOKEN_EXPIRY);
    }

    public String issueRefreshToken(String userId) {
        return buildToken(userId, Map.of(), REFRESH_TOKEN_EXPIRY);
    }

    public String issueVaultToken(String userId) {
        // Note: For Vault, we might still want a separate secret. 
        // Standard JwtEncoder uses one configured key. 
        // For now, let's stick to the primary encoder but add a custom claim.
        return buildToken(userId, Map.of("role", "VAULT", "type", "vault"), VAULT_TOKEN_EXPIRY);
    }

    private String buildToken(String userId, Map<String, Object> extraClaims, long expirySeconds) {
        Instant now = Instant.now();
        
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("nidar-backend-496012")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .subject(userId);

        extraClaims.forEach(claimsBuilder::claim);

        return encoder.encode(JwtEncoderParameters.from(header, claimsBuilder.build())).getTokenValue();
    }

    public String extractUserId(String token) {
        return decoder.decode(token).getSubject();
    }

    public String extractUserIdFromVaultToken(String token) {
        return extractUserId(token);
    }

    public boolean validateToken(String token) {
        try {
            decoder.decode(token);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateVaultToken(String token) {
        return validateToken(token);
    }
}
