package com.dgi.platform.controller;

import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.ChangePasswordRequest;
import com.dgi.platform.dto.UpdateProfileRequest;
import com.dgi.platform.dto.UserProfileDto;
import com.dgi.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile retrieval, updates, and password changes")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileDto profile = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile details")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                                      @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        UserProfileDto updatedProfile = userService.updateProfile(userDetails.getUsername(), updateProfileRequest);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", updatedProfile));
    }

    @PutMapping("/password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<String>> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                               @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        userService.changePassword(userDetails.getUsername(), changePasswordRequest);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
