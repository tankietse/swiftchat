package com.swiftchat.auth_service;

import com.swiftchat.auth_service.config.RepositoryConfig;
import com.swiftchat.shared.security.config.PasswordEncoderConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;

import java.util.Arrays;

@SpringBootApplication(exclude = { OAuth2ClientAutoConfiguration.class })
@ComponentScan(basePackages = {
		"com.swiftchat.auth_service",
		"com.swiftchat.shared.security", // Scan security package from shared module
		"com.swiftchat.shared.utils" // Scan utils package from shared module
}, excludeFilters = {
		@Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.swiftchat.shared.security.config.SecurityConfig.class)
})
@Import({
		RepositoryConfig.class,
		PasswordEncoderConfig.class // Explicitly import PasswordEncoderConfig
})
public class AuthServiceApplication {

	@Autowired
	private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

	@PostConstruct
	public void showConfig() {
		System.out.println("Kafka bootstrap servers: " + env.getProperty("spring.kafka.bootstrap-servers"));
		System.out.println("Active profiles: " + Arrays.toString(env.getActiveProfiles()));
	}
}
