package com.swiftchat.auth_service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;

import java.util.Arrays;

@SpringBootApplication(exclude = { OAuth2ClientAutoConfiguration.class })
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
