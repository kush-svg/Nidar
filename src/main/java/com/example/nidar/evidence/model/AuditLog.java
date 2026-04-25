package com.example.nidar.evidence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Entity
@Table(name = "evidence_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    private String evidenceItemId;
    private String userId;
    private String action;           // UPLOADED, ACCESSED, CONFIRMED, DELETED
    private Long   performedAt;      // epoch seconds
    private String fileHash;         // SHA-256 of the file at time of action
    private String chainHash;        // chain hash at time of action
}
