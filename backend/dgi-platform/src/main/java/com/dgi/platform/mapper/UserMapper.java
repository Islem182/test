package com.dgi.platform.mapper;

import com.dgi.platform.dto.UserProfileDto;
import com.dgi.platform.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileDto toUserProfileDto(User user, String fiscalNumber) {
        if (user == null) return null;
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .enabled(user.isEnabled())
                .isAdmin(user.isAdmin())
                .createdAt(user.getCreatedAt())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .cin(user.getCin())
                .passport(user.getPassport())
                .birthDate(user.getBirthDate())
                .address(user.getAddress())
                .profilePicture(user.getProfilePicture())
                .fiscalNumber(fiscalNumber)
                .build();
    }
}
