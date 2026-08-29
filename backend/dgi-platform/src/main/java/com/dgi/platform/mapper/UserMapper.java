package com.dgi.platform.mapper;

import com.dgi.platform.dto.UserProfileDto;
import com.dgi.platform.entity.Taxpayer;
import com.dgi.platform.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserProfileDto toUserProfileDto(User user, Taxpayer taxpayer, String fiscalNumber) {
        UserProfileDto.UserProfileDtoBuilder builder = UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()));

        if (taxpayer != null) {
            builder.taxpayerId(taxpayer.getId())
                    .firstName(taxpayer.getFirstName())
                    .lastName(taxpayer.getLastName())
                    .cin(taxpayer.getCin())
                    .passport(taxpayer.getPassport())
                    .birthDate(taxpayer.getBirthDate())
                    .address(taxpayer.getAddress())
                    .profilePicture(taxpayer.getProfilePicture())
                    .fiscalNumber(fiscalNumber);
        }

        return builder.build();
    }
}
