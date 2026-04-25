package com.example.nidar.auth.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "vault_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultCredential {

    @Id
    private String id;                  // UUID

    @Column(unique = true, nullable = false)
    private String userId;

    private String hashedPin;           // BCrypt hashed 6-digit PIN
    private Boolean biometricEnabled;   // true if user set up biometric
    private Long    createdAt;
    private Long    updatedAt;          // track PIN changes
}
