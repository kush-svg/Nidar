package com.example.nidar.auth.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.vault-secret}")
    private String vaultSecret;

    private static final long ACCESS_TOKEN_EXPIRY  = 15 * 60 * 1000L;       // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY = 30 * 24 * 60 * 60 * 1000L; // 30 days
    private static final long VAULT_TOKEN_EXPIRY   = 5 * 60 * 1000L;        // 5 minutes

    // ── Issue tokens ──────────────────────────────────────────────────────────
    public String issueAccessToken(String userId, String role) {
        return buildToken(userId, role, ACCESS_TOKEN_EXPIRY, jwtSecret);
    }

    public String issueRefreshToken(String userId) {
        return buildToken(userId, null, REFRESH_TOKEN_EXPIRY, jwtSecret);
    }

    public String issueVaultToken(String userId) {
        // Signed with a separate secret — cannot be used as an app token
        return buildToken(userId, "VAULT", VAULT_TOKEN_EXPIRY, vaultSecret);
    }

    private String buildToken(String userId, String role,
                               long expiry, String secret) {
        Map<String, Object> claims = new HashMap<>();
        if (role != null) claims.put("role", role);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiry))
            .signWith(getSigningKey(secret), SignatureAlgorithm.HS256)
            .compact();
    }

    // ── Validate + extract ────────────────────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            parseClaims(token, jwtSecret);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT validation error (bad key or token format): {}", e.getMessage());
            return false;
        }
    }

    public boolean validateVaultToken(String token) {
        try {
            parseClaims(token, vaultSecret);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid vault JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseClaims(token, jwtSecret).getSubject();
    }

    public String extractUserIdFromVaultToken(String token) {
        return parseClaims(token, vaultSecret).getSubject();
    }

    private Claims parseClaims(String token, String secret) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey(secret))
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSigningKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}