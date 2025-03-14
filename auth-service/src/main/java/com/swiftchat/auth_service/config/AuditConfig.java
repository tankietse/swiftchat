package com.swiftchat.auth_service.config;

import com.swiftchat.auth_service.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * Configuration for JPA entity auditing.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    private final AuthService authService;

    public AuditConfig(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Creates an auditor provider that returns the current user ID.
     *
     * @return AuditorAware implementation that provides the current user ID
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            try {
                return Optional.ofNullable(authService.getCurrentUserId());
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
}
