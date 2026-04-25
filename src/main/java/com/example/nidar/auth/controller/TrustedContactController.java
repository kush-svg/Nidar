package com.example.nidar.auth.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.example.nidar.auth.service.TrustedContactService;
import com.example.nidar.auth.dto.TrustedContactRequest;
import com.example.nidar.auth.dto.TrustedContactResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class TrustedContactController {

    private final TrustedContactService contactService;

    @GetMapping
    public ResponseEntity<List<TrustedContactResponse>> getContacts() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(contactService.getContacts(userId));
    }

    @PostMapping
    public ResponseEntity<TrustedContactResponse> addContact(
        @Valid @RequestBody TrustedContactRequest request
    ) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(contactService.addContact(userId, request));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteContact(
        @PathVariable String contactId
    ) {
        String userId = getCurrentUserId();
        contactService.deleteContact(userId, contactId);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext()
            .getAuthentication().getName();
    }
}
