package com.example.nidar.auth.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.nidar.auth.model.TrustedContact;


@Repository
public interface TrustedContactRepository extends JpaRepository<TrustedContact, String> {

    List<TrustedContact> findByUserId(String userId);

    Optional<TrustedContact> findByUserIdAndPhoneNumber(String userId, String phoneNumber);
}