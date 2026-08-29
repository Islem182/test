package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    private String phone;
    private String firstName;
    private String lastName;
    private String cin;
    private String passport;
    private LocalDate birthDate;
    private String address;
    private String profilePicture;
}
