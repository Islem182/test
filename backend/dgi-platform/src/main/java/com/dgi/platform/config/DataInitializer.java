package com.dgi.platform.config;

import com.dgi.platform.entity.Role;
import com.dgi.platform.entity.RoleName;
import com.dgi.platform.entity.User;
import com.dgi.platform.repository.RoleRepository;
import com.dgi.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Runs once at startup to:
 * 1. Ensure all three roles exist in the roles table.
 * 2. Create the admin account from environment variables if it does not already exist.
 *
 * Required environment variables (set in your OS / Docker / deployment environment):
 *   ADMIN_USERNAME  – the admin login name   (default: "admin" for local dev only)
 *   ADMIN_PASSWORD  – the admin password      (no default — must be set explicitly)
 *   ADMIN_EMAIL     – the admin e-mail        (default: "admin@dgi.gov.tn")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.email:admin@dgi.gov.tn}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedAdminUser();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("DataInitializer: created role {}", roleName);
            }
        }
    }

    private void seedAdminUser() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("DataInitializer: ADMIN_PASSWORD environment variable is not set — skipping admin account creation.");
            return;
        }

        if (userRepository.existsByUsername(adminUsername)) {
            log.info("DataInitializer: admin account '{}' already exists — skipping.", adminUsername);
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found after role seeding"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .roles(roles)
                .build();

        userRepository.save(admin);
        // Password is intentionally NOT logged
        log.info("DataInitializer: admin account '{}' created successfully.", adminUsername);
    }
}
