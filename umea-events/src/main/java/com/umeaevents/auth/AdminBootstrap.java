package com.umeaevents.auth;

import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a first ADMIN account on startup when {@code app.bootstrap-admin.email} and
 * {@code app.bootstrap-admin.password} are set and that account doesn't already exist.
 *
 * <p>Self-registration cannot grant ADMIN (see AuthService), so a fresh production database
 * would otherwise have no admin able to log in. Idempotent: skips if the email already exists
 * or if the properties are unset. Credentials come from env vars, never the codebase.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("Admin bootstrap skipped (app.bootstrap-admin.email/password not set).");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.info("Admin bootstrap skipped — user '{}' already exists.", email);
            return;
        }
        User admin = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Admin bootstrap: created ADMIN account '{}'.", email);
    }
}
