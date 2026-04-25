package com.example.nidar.evidence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.example.nidar.evidence.model.LocationSnapshot;

@Repository
public interface LocationSnapshotRepository extends JpaRepository<LocationSnapshot, String> {

    List<LocationSnapshot> findBySosSessionIdOrderByCapturedAtAsc(String sosSessionId);

    Optional<LocationSnapshot> findTopBySosSessionIdOrderByCapturedAtDesc(String sosSessionId);

    List<LocationSnapshot> findByUserId(String userId);
}
