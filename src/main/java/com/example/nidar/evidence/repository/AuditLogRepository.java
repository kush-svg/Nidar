package com.example.nidar.evidence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.example.nidar.evidence.model.AuditLog;
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByEvidenceItemIdOrderByPerformedAtAsc(String evidenceItemId);

    List<AuditLog> findByUserIdOrderByPerformedAtAsc(String userId);
}
