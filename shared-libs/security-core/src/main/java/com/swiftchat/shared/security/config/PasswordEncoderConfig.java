package com.swiftchat.shared.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for providing a PasswordEncoder bean.
 * Separated from SecurityConfig to avoid circular dependencies.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Provides the password encoder.
     *
     * @return BCryptPasswordEncoder for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
