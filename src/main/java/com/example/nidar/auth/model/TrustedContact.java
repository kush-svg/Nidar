package com.example.nidar.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "trusted_contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustedContact {

    @Id
    private String id;                  // UUID

    private String userId;              // owner — the woman who added this contact
    private String name;                // contact's display name
    private String phoneNumber;         // format: 91XXXXXXXXXX
    private String fcmToken;            // null if contact doesn't have the app

    private Long   createdAt;
}