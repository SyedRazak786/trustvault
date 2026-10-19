package com.trustvault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseCheck {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCheck.class);

    // Runs once at startup. If the database is unreachable or the password is wrong, startup fails with a clear error.
    @Bean
    CommandLineRunner verifyDatabase(JdbcTemplate jdbc) {
        return args -> {
            Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            log.info("Database connection OK. users table has {} rows.", users);
        };
    }
}