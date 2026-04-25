package com.example.nidar.sos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.nidar.sos.model.SosSession;
import com.example.nidar.sos.model.SosStatus;


// sos/repository/SosSessionRepository.java
@Repository
public interface SosSessionRepository extends JpaRepository<SosSession, String> {

    // Find all active sessions for a user
    // Used to prevent duplicate SOS triggers
    List<SosSession> findByUserIdAndStatus(String userId, SosStatus status);

    // Find sessions older than a cutoff — for cleanup jobs
    List<SosSession> findByStatusAndTriggeredAtBefore(SosStatus status, long cutoffEpoch);

    Optional<SosSession> findBySessionId(String sessionId);
}