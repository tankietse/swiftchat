package com.swiftchat.auth_service.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.swiftchat.auth_service.repository")
@EntityScan(basePackages = {
        "com.swiftchat.auth_service.model",
        "com.swiftchat.shared.security.model"
})
@EnableTransactionManagement
@EnableJpaAuditing
public class RepositoryConfig {
    // Additional configuration if needed
}
