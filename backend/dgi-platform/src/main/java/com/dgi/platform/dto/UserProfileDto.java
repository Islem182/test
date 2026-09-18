package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private boolean enabled;
    private boolean isAdmin;
    private LocalDateTime createdAt;

    // Identity / taxpayer fields
    private String firstName;
    private String lastName;
    private String cin;
    private String passport;
    private LocalDate birthDate;
    private String address;
    private String profilePicture;
    private String fiscalNumber;
}
