package com.example.nidar.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.example.nidar.auth.model.VaultCredential;

@Repository
public interface VaultCredentialRepository extends JpaRepository<VaultCredential, String> {
    Optional<VaultCredential> findByUserId(String userId);
}
