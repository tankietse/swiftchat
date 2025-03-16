package com.swiftchat.auth_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Auth Service Application Tests")
class AuthServiceApplicationTests {

	@Test
	@DisplayName("Context loads successfully")
	void contextLoads() {
		// This test verifies that the Spring application context loads successfully
	}
}
