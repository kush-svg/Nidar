package com.example.nidar.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

// auth/model/User.java
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;                  // UUID

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;         // format: 91XXXXXXXXXX

    private String name;

    @Column(name = "fcm_token")
    private String fcmToken;            // updated on every app launch

    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private org.locationtech.jts.geom.Point geom;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;              // USER or PROTECTOR

    @Column(name = "h3_index")
    private String  h3Index;            // last known location cell — for SOS protector query
    private Boolean isActive;           // false if account deactivated

    @Column(name = "last_seen_at")
    private Long    lastSeenAt;         // epoch seconds — updated on every API call

    @Column(name = "created_at")
    private Long    createdAt;
}