package com.example.nidar.evidence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import com.example.nidar.evidence.model.EvidenceItem;
import com.example.nidar.evidence.model.EvidenceStatus;
@Repository
public interface EvidenceRepository extends JpaRepository<EvidenceItem, String> {

    List<EvidenceItem> findByUserIdAndStatusOrderByUploadedAtDesc(
        String userId, EvidenceStatus status
    );

    List<EvidenceItem> findByUserIdOrderByUploadedAtDesc(String userId);

    Optional<EvidenceItem> findTopByUserIdOrderByUploadedAtDesc(String userId);

    List<EvidenceItem> findBySosSessionId(String sosSessionId);

    // For cleanup job — finds auto-captured items older than 24h still pending
    @Query("""
        SELECT e FROM EvidenceItem e
        WHERE e.status = 'PENDING_REVIEW'
          AND e.captureMode = 'AUTO'
          AND e.uploadedAt < :cutoff
        """)
    List<EvidenceItem> findExpiredPendingItems(@Param("cutoff") long cutoff);
}
