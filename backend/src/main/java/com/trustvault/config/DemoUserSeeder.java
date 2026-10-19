package com.trustvault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("demo")
public class DemoUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    // Runs only with the demo profile. Creates the three demo accounts once, when the users table is empty.
    @Bean
    CommandLineRunner seedDemoUsers(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return args -> {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            if (count != null && count > 0) {
                log.info("Demo users already exist, skipping seed.");
                return;
            }
            insert(jdbc, encoder, "admin@trustvault.test", "ADMIN", System.getenv("DEMO_ADMIN_PASSWORD"));
            insert(jdbc, encoder, "owner@trustvault.test", "OWNER", System.getenv("DEMO_OWNER_PASSWORD"));
            insert(jdbc, encoder, "viewer@trustvault.test", "VIEWER", System.getenv("DEMO_VIEWER_PASSWORD"));
            log.info("Demo users created.");
        };
    }

    private void insert(JdbcTemplate jdbc, PasswordEncoder encoder, String email, String role, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set the demo password environment variable for " + role);
        }
        jdbc.update("INSERT INTO users (email, password_hash, role, active) VALUES (?, ?, ?, TRUE)",
                email, encoder.encode(password), role);
    }
}