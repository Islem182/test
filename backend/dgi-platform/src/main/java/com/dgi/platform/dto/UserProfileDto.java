package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private LocalDateTime createdAt;
    private List<String> roles;

    // Taxpayer profile fields (if user is TAXPAYER)
    private Long taxpayerId;
    private String firstName;
    private String lastName;
    private String cin;
    private String passport;
    private LocalDate birthDate;
    private String address;
    private String profilePicture;
    private String fiscalNumber;
}
