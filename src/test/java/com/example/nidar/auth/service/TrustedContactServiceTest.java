package com.example.nidar.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.auth.model.TrustedContact;
import com.example.nidar.auth.model.User;
import com.example.nidar.auth.repository.TrustedContactRepository;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.auth.dto.TrustedContactRequest;
import com.example.nidar.auth.dto.TrustedContactResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustedContactServiceTest {

    @Mock
    private TrustedContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TrustedContactService trustedContactService;

    @Test
    void getContacts_ReturnsListOfContactResponses() {
        TrustedContact c1 = TrustedContact.builder()
            .id("c1").userId("u1").name("Mom").phoneNumber("919111111111")
            .fcmToken("fcm1").createdAt(100L).build();
        TrustedContact c2 = TrustedContact.builder()
            .id("c2").userId("u1").name("Dad").phoneNumber("919222222222")
            .fcmToken(null).createdAt(200L).build();

        when(contactRepository.findByUserId("u1")).thenReturn(List.of(c1, c2));

        List<TrustedContactResponse> result = trustedContactService.getContacts("u1");

        assertEquals(2, result.size());
        assertEquals("Mom", result.get(0).name());
        assertEquals("Dad", result.get(1).name());
    }

    @Test
    void getContacts_WhenNoContacts_ReturnsEmptyList() {
        when(contactRepository.findByUserId("u1")).thenReturn(List.of());

        List<TrustedContactResponse> result = trustedContactService.getContacts("u1");

        assertTrue(result.isEmpty());
    }

    @Test
    void addContact_WhenContactIsNidarUser_IncludesFcmToken() {
        when(contactRepository.findByUserIdAndPhoneNumber("u1", "919111111111"))
            .thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("919111111111"))
            .thenReturn(Optional.of(User.builder().fcmToken("contact-fcm").build()));
        when(contactRepository.save(any(TrustedContact.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        TrustedContactRequest request = new TrustedContactRequest("Mom", "9111111111");

        TrustedContactResponse response = trustedContactService.addContact("u1", request);

        assertEquals("Mom", response.name());
        assertEquals("919111111111", response.phoneNumber());
        assertEquals("contact-fcm", response.fcmToken());
    }

    @Test
    void addContact_WhenContactIsNotNidarUser_FcmTokenIsNull() {
        when(contactRepository.findByUserIdAndPhoneNumber("u1", "919111111111"))
            .thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("919111111111"))
            .thenReturn(Optional.empty());
        when(contactRepository.save(any(TrustedContact.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        TrustedContactRequest request = new TrustedContactRequest("Friend", "9111111111");

        TrustedContactResponse response = trustedContactService.addContact("u1", request);

        assertNull(response.fcmToken());
    }

    @Test
    void addContact_WhenContactAlreadyExists_ThrowsException() {
        TrustedContact existing = TrustedContact.builder().id("c1").build();
        when(contactRepository.findByUserIdAndPhoneNumber("u1", "919111111111"))
            .thenReturn(Optional.of(existing));

        TrustedContactRequest request = new TrustedContactRequest("Mom", "9111111111");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> trustedContactService.addContact("u1", request));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void deleteContact_WhenOwnerDeletes_DeletesSuccessfully() {
        TrustedContact contact = TrustedContact.builder()
            .id("c1").userId("u1").build();
        when(contactRepository.findById("c1")).thenReturn(Optional.of(contact));

        trustedContactService.deleteContact("u1", "c1");

        verify(contactRepository).delete(contact);
    }

    @Test
    void deleteContact_WhenNonOwnerDeletes_ThrowsUnauthorized() {
        TrustedContact contact = TrustedContact.builder()
            .id("c1").userId("u1").build();
        when(contactRepository.findById("c1")).thenReturn(Optional.of(contact));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> trustedContactService.deleteContact("other-user", "c1"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void deleteContact_WhenContactNotFound_ThrowsException() {
        when(contactRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> trustedContactService.deleteContact("u1", "nonexistent"));
    }
}
