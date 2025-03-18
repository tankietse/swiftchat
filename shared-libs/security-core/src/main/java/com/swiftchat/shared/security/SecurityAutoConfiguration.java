package com.swiftchat.shared.security;

import com.swiftchat.shared.security.config.PasswordEncoderConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration class for the security module.
 * This ensures all necessary beans are created and components are scanned.
 */
@Configuration
@ComponentScan(basePackages = "com.swiftchat.shared.security")
@Import(PasswordEncoderConfig.class)
public class SecurityAutoConfiguration {
    // Configuration is handled by annotations
}
