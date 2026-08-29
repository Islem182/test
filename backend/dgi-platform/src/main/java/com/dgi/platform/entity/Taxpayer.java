package com.dgi.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "taxpayers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taxpayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 20)
    private String cin;

    @Column(unique = true, length = 30)
    private String passport;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "profile_picture")
    private String profilePicture;
}
