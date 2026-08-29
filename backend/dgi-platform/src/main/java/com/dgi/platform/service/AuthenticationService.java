package com.dgi.platform.service;

import com.dgi.platform.dto.*;

public interface AuthenticationService {
    JwtResponse register(RegisterRequest registerRequest);
    JwtResponse login(LoginRequest loginRequest);
    void logout(String token);
    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
    void forgotPassword(ForgotPasswordRequest forgotPasswordRequest);
    void resetPassword(ResetPasswordRequest resetPasswordRequest);
}
