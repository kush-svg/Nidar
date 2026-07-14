package com.example.nidar.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.UserRole;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.auth.dto.UpdateProfileRequest;
import com.example.nidar.auth.dto.UserProfileResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private User sampleUser() {
        return User.builder()
            .id("u1")
            .phoneNumber("919999999999")
            .name("Kush")
            .role(UserRole.USER)
            .isActive(true)
            .createdAt(1000000L)
            .lastSeenAt(2000000L)
            .build();
    }

    @Test
    void getProfile_WhenUserExists_ReturnsProfileResponse() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));

        UserProfileResponse response = userProfileService.getProfile("u1");

        assertEquals("u1", response.id());
        assertEquals("919999999999", response.phoneNumber());
        assertEquals("Kush", response.name());
        assertEquals("USER", response.role());
        assertTrue(response.isActive());
    }

    @Test
    void getProfile_WhenUserDoesNotExist_ThrowsException() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> userProfileService.getProfile("missing"));
    }

    @Test
    void updateProfile_UpdatesNameOnly() {
        User user = sampleUser();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("NewName", null, null, null);

        UserProfileResponse response = userProfileService.updateProfile("u1", request);

        assertEquals("NewName", response.name());
        assertEquals("USER", response.role());  // unchanged
    }

    @Test
    void updateProfile_UpdatesRoleToProtector() {
        User user = sampleUser();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "PROTECTOR", null);

        UserProfileResponse response = userProfileService.updateProfile("u1", request);

        assertEquals("PROTECTOR", response.role());
        assertEquals("Kush", response.name()); // unchanged
    }

    @Test
    void updateProfile_UpdatesFcmTokenAndH3Index() {
        User user = sampleUser();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(null, "fcm-token-123", null, "h3cell456");

        userProfileService.updateProfile("u1", request);

        assertEquals("fcm-token-123", user.getFcmToken());
        assertEquals("h3cell456", user.getH3Index());
    }

    @Test
    void updateProfile_IgnoresBlankName() {
        User user = sampleUser();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("   ", null, null, null);

        UserProfileResponse response = userProfileService.updateProfile("u1", request);

        assertEquals("Kush", response.name()); // blank name should be ignored
    }

    @Test
    void deactivateAccount_SetsIsActiveToFalse() {
        User user = sampleUser();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        userProfileService.deactivateAccount("u1");

        assertFalse(user.getIsActive());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateAccount_WhenUserDoesNotExist_ThrowsException() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> userProfileService.deactivateAccount("missing"));
    }
}
