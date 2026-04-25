package com.example.nidar.auth.dto;

import com.example.nidar.auth.model.TrustedContact;

public record TrustedContactResponse(
    String id,
    String name,
    String phoneNumber,
    String fcmToken,
    Long   createdAt
) {
    public static TrustedContactResponse from(TrustedContact c) {
        return new TrustedContactResponse(
            c.getId(), c.getName(),
            c.getPhoneNumber(), c.getFcmToken(),
            c.getCreatedAt()
        );
    }
}
