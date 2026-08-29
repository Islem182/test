package com.dgi.platform.service.impl;

import com.dgi.platform.dto.*;
import com.dgi.platform.entity.Role;
import com.dgi.platform.entity.RoleName;
import com.dgi.platform.entity.Taxpayer;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.repository.RoleRepository;
import com.dgi.platform.repository.TaxpayerRepository;
import com.dgi.platform.repository.UserRepository;
import com.dgi.platform.security.JwtTokenProvider;
import com.dgi.platform.security.UserPrincipal;
import com.dgi.platform.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public JwtResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BadRequestException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email Address already in use!");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phone(registerRequest.getPhone())
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        RoleName targetRoleName;
        try {
            targetRoleName = RoleName.valueOf("ROLE_" + registerRequest.getRole().toUpperCase().replace("ROLE_", ""));
        } catch (IllegalArgumentException e) {
            targetRoleName = RoleName.ROLE_TAXPAYER;
        }

        Role userRole = roleRepository.findByName(targetRoleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_TAXPAYER).build()));

        user.getRoles().add(userRole);
        User savedUser = userRepository.save(user);

        // If user is TAXPAYER, create associated Taxpayer profile
        if (targetRoleName == RoleName.ROLE_TAXPAYER) {
            Taxpayer taxpayer = Taxpayer.builder()
                    .user(savedUser)
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .cin(registerRequest.getCin())
                    .passport(registerRequest.getPassport())
                    .birthDate(registerRequest.getBirthDate())
                    .address(registerRequest.getAddress())
                    .build();
            taxpayerRepository.save(taxpayer);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getUsername());

        List<String> roles = savedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getUsername());

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .id(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    public void logout(String token) {
        SecurityContextHolder.clearContext();
    }

    @Override
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String requestRefreshToken = refreshTokenRequest.getRefreshToken();
        if (tokenProvider.validateToken(requestRefreshToken)) {
            String username = tokenProvider.getUsernameFromJWT(requestRefreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toList();

            String newAccessToken = tokenProvider.generateTokenFromUsername(user.getUsername(), user.getId(), String.join(",", roles));
            String newRefreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            return JwtResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(roles)
                    .build();
        }
        throw new BadRequestException("Invalid or expired Refresh Token");
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        User user = userRepository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", forgotPasswordRequest.getEmail()));
        // Simulated password reset token generated/sent to user email
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        if (!tokenProvider.validateToken(resetPasswordRequest.getToken())) {
            throw new BadRequestException("Invalid or expired password reset token");
        }
        String username = tokenProvider.getUsernameFromJWT(resetPasswordRequest.getToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepository.save(user);
    }
}
