package com.example.nidar.sos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.auth.model.TrustedContact;
import com.example.nidar.auth.model.User;
import com.example.nidar.auth.model.UserRole;
import com.example.nidar.auth.repository.TrustedContactRepository;
import com.example.nidar.auth.repository.UserRepository;
import com.example.nidar.common.util.H3SnapUtil;
import com.example.nidar.common.messaging.KafkaMessagingService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SosAlertServiceTest {

    @Mock
    private TrustedContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FcmService fcmService;

    @Mock
    private SmsService smsService;

    @Mock
    private H3SnapUtil h3SnapUtil;

    @Mock
    private KafkaMessagingService kafkaMessagingService;

    @InjectMocks
    private SosAlertService sosAlertService;

    @Test
    void alertTrustedContacts_PublishesToPubSub() {
        when(contactRepository.findByUserId("u1")).thenReturn(List.of());

        sosAlertService.alertTrustedContacts("u1", "s1", 28.6139, 77.2090);

        verify(kafkaMessagingService).publishSosAlert("s1", "u1", 28.6139, 77.2090);
    }

    @Test
    void alertTrustedContacts_WhenNoContacts_DoesNotSendNotifications() {
        when(contactRepository.findByUserId("u1")).thenReturn(List.of());

        sosAlertService.alertTrustedContacts("u1", "s1", 28.6139, 77.2090);

        verifyNoInteractions(fcmService);
        verifyNoInteractions(smsService);
    }

    @Test
    void alertTrustedContacts_SendsFcmAndSmsToEachContact() {
        TrustedContact contact = TrustedContact.builder()
            .userId("u1").name("Mom")
            .phoneNumber("919111111111")
            .fcmToken("contact-fcm-token")
            .build();

        when(contactRepository.findByUserId("u1")).thenReturn(List.of(contact));
        when(userRepository.findNameById("u1")).thenReturn("Kush");

        sosAlertService.alertTrustedContacts("u1", "s1", 28.6139, 77.2090);

        verify(fcmService).sendHighPriority(
            eq("contact-fcm-token"),
            eq("SOS ALERT"),
            contains("Kush"),
            any()
        );
        verify(smsService).send(eq("919111111111"), contains("Kush"));
    }

    @Test
    void alertTrustedContacts_SkipsFcm_WhenFcmTokenIsNull() {
        TrustedContact contact = TrustedContact.builder()
            .userId("u1").name("Mom")
            .phoneNumber("919111111111")
            .fcmToken(null)
            .build();

        when(contactRepository.findByUserId("u1")).thenReturn(List.of(contact));
        when(userRepository.findNameById("u1")).thenReturn("Kush");

        sosAlertService.alertTrustedContacts("u1", "s1", 28.6139, 77.2090);

        verify(fcmService, never()).sendHighPriority(any(), any(), any(), any());
        verify(smsService).send(eq("919111111111"), anyString());
    }

    @Test
    void alertTrustedContacts_SkipsSms_WhenPhoneIsNull() {
        TrustedContact contact = TrustedContact.builder()
            .userId("u1").name("Mom")
            .phoneNumber(null)
            .fcmToken("fcm-token")
            .build();

        when(contactRepository.findByUserId("u1")).thenReturn(List.of(contact));
        when(userRepository.findNameById("u1")).thenReturn("Kush");

        sosAlertService.alertTrustedContacts("u1", "s1", 28.6139, 77.2090);

        verify(fcmService).sendHighPriority(any(), any(), any(), any());
        verify(smsService, never()).send(any(), any());
    }

    @Test
    void alertNearbyProtectors_FindsProtectorsInNeighborCells() {
        when(h3SnapUtil.getNeighborCells("h3cell123", 1))
            .thenReturn(List.of("neighbor1", "neighbor2"));

        User protector = User.builder()
            .id("p1").fcmToken("protector-fcm").role(UserRole.PROTECTOR).build();

        when(userRepository.findActiveProtectorsInCells(anyList(), anyLong()))
            .thenReturn(List.of(protector));

        sosAlertService.alertNearbyProtectors("h3cell123", "s1", 28.6139, 77.2090);

        verify(fcmService).sendSilentData(eq("protector-fcm"), any());
    }

    @Test
    void alertNearbyProtectors_WhenNoProtectors_DoesNotSendNotifications() {
        when(h3SnapUtil.getNeighborCells("h3cell123", 1))
            .thenReturn(List.of("neighbor1"));
        when(userRepository.findActiveProtectorsInCells(anyList(), anyLong()))
            .thenReturn(List.of());

        sosAlertService.alertNearbyProtectors("h3cell123", "s1", 28.6139, 77.2090);

        verifyNoInteractions(fcmService);
    }

    @Test
    void alertNearbyProtectors_SkipsProtectorsWithoutFcmToken() {
        when(h3SnapUtil.getNeighborCells("h3cell123", 1))
            .thenReturn(List.of("neighbor1"));

        User protectorNoToken = User.builder()
            .id("p1").fcmToken(null).role(UserRole.PROTECTOR).build();

        when(userRepository.findActiveProtectorsInCells(anyList(), anyLong()))
            .thenReturn(List.of(protectorNoToken));

        sosAlertService.alertNearbyProtectors("h3cell123", "s1", 28.6139, 77.2090);

        verify(fcmService, never()).sendSilentData(any(), any());
    }
}
