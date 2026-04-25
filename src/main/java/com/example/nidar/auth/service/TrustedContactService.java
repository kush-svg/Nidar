package com.example.nidar.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nidar.auth.model.TrustedContact;
import com.example.nidar.auth.model.User;
import com.example.nidar.auth.repository.TrustedContactRepository;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.auth.dto.TrustedContactRequest;
import com.example.nidar.auth.dto.TrustedContactResponse;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrustedContactService {

    private final TrustedContactRepository contactRepository;
    private final UserRepository           userRepository;

    public List<TrustedContactResponse> getContacts(String userId) {
        return contactRepository.findByUserId(userId)
            .stream()
            .map(TrustedContactResponse::from)
            .toList();
    }

    @Transactional
    public TrustedContactResponse addContact(
        String userId,
        TrustedContactRequest request
    ) {
        // Check if contact already exists
        contactRepository.findByUserIdAndPhoneNumber(
            userId, "91" + request.phoneNumber()
        ).ifPresent(c -> {
            throw new RuntimeException("Contact already exists");
        });

        // Check if contact is a Nidar user — get their FCM token
        String fcmToken = userRepository
            .findByPhoneNumber("91" + request.phoneNumber())
            .map(User::getFcmToken)
            .orElse(null);

        TrustedContact contact = TrustedContact.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .name(request.name())
            .phoneNumber("91" + request.phoneNumber())
            .fcmToken(fcmToken)
            .createdAt(Instant.now().getEpochSecond())
            .build();

        return TrustedContactResponse.from(
            contactRepository.save(contact)
        );
    }

    @Transactional
    public void deleteContact(String userId, String contactId) {
        TrustedContact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Only owner can delete
        if (!contact.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        contactRepository.delete(contact);
    }
}
