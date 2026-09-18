package com.dgi.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Single user table — combines authentication and taxpayer identity.
 *
 * is_admin = true  → the ONE admin account (created by DataInitializer from env vars).
 * is_admin = false → every normal taxpayer registration.
 *
 * Roles, user_roles, and the separate taxpayers table have been removed.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    // ── Identity / taxpayer fields ────────────────────────────────────────────

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    /** Tunisian CIN — unique, nullable (admin has no CIN). */
    @Column(unique = true, length = 20)
    private String cin;

    /** Passport number — unique, nullable. */
    @Column(unique = true, length = 30)
    private String passport;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_picture")
    private String profilePicture;

    // ── Security / admin flag ─────────────────────────────────────────────────

    /** true ONLY for the single admin account. All registered taxpayers use false. */
    @Builder.Default
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
