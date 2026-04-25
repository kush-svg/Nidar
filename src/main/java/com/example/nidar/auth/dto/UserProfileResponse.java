package com.example.nidar.auth.dto;

import com.example.nidar.auth.model.User;

public record UserProfileResponse(
    String  id,
    String  phoneNumber,
    String  name,
    String  role,
    Boolean isActive,
    Long    createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getPhoneNumber(),
            user.getName(),
            user.getRole().name(),
            user.getIsActive(),
            user.getCreatedAt()
        );
    }
}
