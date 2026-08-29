package com.dgi.platform.controller;

import com.dgi.platform.dto.*;
import com.dgi.platform.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User Registration, Login, JWT Token management & Password Recovery")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register new user/taxpayer account")
    public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        JwtResponse response = authenticationService.register(registerRequest);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authenticationService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate security context")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        authenticationService.logout(bearerToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Obtain new JWT access token using valid refresh token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        JwtResponse response = authenticationService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset link for forgotten password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        authenticationService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions have been sent if account exists."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        authenticationService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully."));
    }
}
