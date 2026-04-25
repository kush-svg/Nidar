package com.example.nidar.auth.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.nidar.auth.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // ST_DWithin — finds protectors within exact 2km radius
    // Accounts for Earth's curvature via ::geography cast
    @Query(value = """
        SELECT * FROM users
        WHERE ST_DWithin(
            geom::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            2000
        )
        AND role = 'PROTECTOR'
        AND is_active = true
        AND last_seen_at > :cutoff
        """, nativeQuery = true)
    List<User> findProtectorsWithin2km(
        @Param("lat")    double lat,
        @Param("lng")    double lng,
        @Param("cutoff") long   cutoff
    );

    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u.name FROM User u WHERE u.id = :userId")
    String findNameById(@Param("userId") String userId);

    @Query("""
        SELECT u FROM User u
        WHERE u.h3Index IN :cells
          AND u.role = 'PROTECTOR'
          AND u.isActive = true
          AND u.lastSeenAt > :cutoff
        """)
    List<User> findActiveProtectorsInCells(
        @Param("cells")  List<String> cells,
        @Param("cutoff") long cutoff
    );
}