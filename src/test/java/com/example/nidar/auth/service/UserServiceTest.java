package com.example.nidar.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.UserRole;
import com.example.nidar.auth.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void existsByPhone_WhenUserExists_ReturnsTrue() {
        when(userRepository.findByPhoneNumber("919999999999"))
            .thenReturn(Optional.of(User.builder().id("u1").build()));

        assertTrue(userService.existsByPhone("919999999999"));
    }

    @Test
    void existsByPhone_WhenUserDoesNotExist_ReturnsFalse() {
        when(userRepository.findByPhoneNumber("919999999999"))
            .thenReturn(Optional.empty());

        assertFalse(userService.existsByPhone("919999999999"));
    }

    @Test
    void getOrCreate_WhenUserExists_ReturnsExistingUser() {
        User existing = User.builder()
            .id("existing-id")
            .phoneNumber("919999999999")
            .role(UserRole.USER)
            .build();

        when(userRepository.findByPhoneNumber("919999999999"))
            .thenReturn(Optional.of(existing));

        User result = userService.getOrCreate("919999999999");

        assertEquals("existing-id", result.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrCreate_WhenUserDoesNotExist_CreatesNewUser() {
        when(userRepository.findByPhoneNumber("919999999999"))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        User result = userService.getOrCreate("919999999999");

        assertNotNull(result.getId());
        assertEquals("919999999999", result.getPhoneNumber());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.getIsActive());
        assertNotNull(result.getCreatedAt());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getById_WhenUserExists_ReturnsUser() {
        User user = User.builder().id("u1").phoneNumber("919999999999").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        User result = userService.getById("u1");

        assertEquals("u1", result.getId());
    }

    @Test
    void getById_WhenUserDoesNotExist_ThrowsException() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> userService.getById("nonexistent"));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void updateLastSeen_WhenUserExists_UpdatesTimestampAndH3Index() {
        User user = User.builder()
            .id("u1")
            .lastSeenAt(0L)
            .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        userService.updateLastSeen("u1", "h3cell123");

        assertNotEquals(0L, user.getLastSeenAt());
        assertEquals("h3cell123", user.getH3Index());
        verify(userRepository).save(user);
    }

    @Test
    void updateLastSeen_WhenH3IndexIsNull_DoesNotUpdateH3Index() {
        User user = User.builder()
            .id("u1")
            .h3Index("oldCell")
            .lastSeenAt(0L)
            .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        userService.updateLastSeen("u1", null);

        assertEquals("oldCell", user.getH3Index());
        verify(userRepository).save(user);
    }
}
