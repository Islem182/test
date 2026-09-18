package com.dgi.platform.service.impl;

import com.dgi.platform.dto.*;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.repository.UserRepository;
import com.dgi.platform.security.JwtTokenProvider;
import com.dgi.platform.security.UserPrincipal;
import com.dgi.platform.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public JwtResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new BadRequestException("Username is already taken!");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new BadRequestException("Email Address already in use!");

        // Blank CIN/Passport → null to avoid unique constraint violations
        String cin      = blank(req.getCin())      ? null : req.getCin().trim();
        String passport = blank(req.getPassport()) ? null : req.getPassport().trim();

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .cin(cin)
                .passport(passport)
                .birthDate(req.getBirthDate())
                .address(req.getAddress())
                .phone(req.getPhone())
                .isAdmin(false)          // registrations are NEVER admin
                .enabled(true)
                .build();

        User saved = userRepository.save(user);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt     = tokenProvider.generateToken(auth);
        String refresh = tokenProvider.generateRefreshToken(saved.getUsername());

        return JwtResponse.builder()
                .accessToken(jwt).refreshToken(refresh).tokenType("Bearer")
                .id(saved.getId()).username(saved.getUsername())
                .email(saved.getEmail()).isAdmin(false)
                .build();
    }

    @Override
    public JwtResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String jwt     = tokenProvider.generateToken(auth);
        String refresh = tokenProvider.generateRefreshToken(principal.getUsername());

        return JwtResponse.builder()
                .accessToken(jwt).refreshToken(refresh).tokenType("Bearer")
                .id(principal.getId()).username(principal.getUsername())
                .email(principal.getEmail()).isAdmin(principal.isAdmin())
                .build();
    }

    @Override
    public void logout(String token) {
        SecurityContextHolder.clearContext();
    }

    @Override
    public JwtResponse refreshToken(RefreshTokenRequest req) {
        if (!tokenProvider.validateToken(req.getRefreshToken()))
            throw new BadRequestException("Invalid or expired Refresh Token");

        String username = tokenProvider.getUsernameFromJWT(req.getRefreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        String newAccess  = tokenProvider.generateTokenFromUsername(user.getUsername(), user.getId(), user.isAdmin());
        String newRefresh = tokenProvider.generateRefreshToken(user.getUsername());

        return JwtResponse.builder()
                .accessToken(newAccess).refreshToken(newRefresh).tokenType("Bearer")
                .id(user.getId()).username(user.getUsername())
                .email(user.getEmail()).isAdmin(user.isAdmin())
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", req.getEmail()));
        // Password reset email sending (simulated)
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!tokenProvider.validateToken(req.getToken()))
            throw new BadRequestException("Invalid or expired password reset token");
        String username = tokenProvider.getUsernameFromJWT(req.getToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
