package com.dgi.platform.service;

import com.dgi.platform.dto.ChangePasswordRequest;
import com.dgi.platform.dto.UpdateProfileRequest;
import com.dgi.platform.dto.UserProfileDto;

public interface UserService {
    UserProfileDto getProfile(String username);
    UserProfileDto updateProfile(String username, UpdateProfileRequest updateProfileRequest);
    void changePassword(String username, ChangePasswordRequest changePasswordRequest);
}
