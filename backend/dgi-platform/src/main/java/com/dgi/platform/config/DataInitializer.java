package com.dgi.platform.config;

import com.dgi.platform.entity.User;
import com.dgi.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

/**
 * Runs on every startup.
 *
 * 1. cleanupEmptyUniqueFields()   — fixes old "" values → NULL in users.cin / users.passport
 * 2. seedAdminUser()              — creates the ONE admin from env vars if missing
 * 3. checkTessdataConfiguration() — warns if OCR language files are absent
 *
 * No role seeding — the role system has been removed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.email:admin@dgi.gov.tn}")
    private String adminEmail;

    @Value("${ocr.tessdata-path:}")
    private String tessdataPath;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        cleanupEmptyUniqueFields();
        seedAdminUser();
        checkTessdataConfiguration();
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Convert "" → NULL for unique-constrained columns that were saved blank. */
    private void cleanupEmptyUniqueFields() {
        try {
            int cinFixed      = jdbcTemplate.update("UPDATE users SET cin      = NULL WHERE cin      = ''");
            int passportFixed = jdbcTemplate.update("UPDATE users SET passport = NULL WHERE passport = ''");
            if (cinFixed > 0 || passportFixed > 0)
                log.info("DataInitializer: fixed {} empty cin, {} empty passport → NULL",
                        cinFixed, passportFixed);
        } catch (Exception e) {
            log.warn("DataInitializer: cleanup skipped — {}", e.getMessage());
        }
    }

    private void seedAdminUser() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("DataInitializer: ADMIN_PASSWORD not set — skipping admin creation.");
            return;
        }
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("DataInitializer: admin '{}' already exists.", adminUsername);
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .isAdmin(true)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("DataInitializer: admin '{}' created.", adminUsername);
    }

    private void checkTessdataConfiguration() {
        String path = (tessdataPath != null && !tessdataPath.isBlank())
                ? tessdataPath : System.getenv("TESSDATA_PREFIX");
        if (path == null || path.isBlank()) {
            log.warn("OCR: TESSDATA_PATH not set — Tesseract will use its own default.");
            return;
        }
        boolean fra = new File(path, "fra.traineddata").exists();
        boolean ara = new File(path, "ara.traineddata").exists();
        boolean eng = new File(path, "eng.traineddata").exists();
        if (!fra || !ara || !eng)
            log.warn("OCR: missing tessdata in '{}' — fra={} ara={} eng={}", path, fra, ara, eng);
        else
            log.info("DataInitializer: Tesseract tessdata OK in '{}'.", path);
    }
}
