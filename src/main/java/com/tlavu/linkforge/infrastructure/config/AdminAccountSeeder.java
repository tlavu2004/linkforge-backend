package com.tlavu.linkforge.infrastructure.config;

import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.repository.UserRepository;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@linkforge.com";
        String adminPassword = "adminpassword";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.create(
                    TSID.fast().toLong(),
                    "Admin",
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN);
            userRepository.save(admin);
            log.info("Default admin account created with email: {}", adminEmail);
        } else {
            log.info("Admin account already exists.");
        }
    }
}
