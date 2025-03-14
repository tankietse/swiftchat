package com.swiftchat.auth_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.google", name = "client-id", matchIfMissing = false)
public class OAuth2Config {

    // This configuration only activates if Google OAuth2 client ID is provided
    // Otherwise, standard authentication will be used
}
