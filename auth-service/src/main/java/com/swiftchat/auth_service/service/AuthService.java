package com.swiftchat.auth_service.service;

import com.swiftchat.auth_service.dto.AuthResponseDto;
import com.swiftchat.auth_service.dto.LoginRequestDto;
import com.swiftchat.auth_service.dto.RegisterRequestDto;
import com.swiftchat.auth_service.dto.TokenRefreshRequestDto;
import com.swiftchat.shared.security.model.User;

import java.util.Map;
import java.util.UUID;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    AuthResponseDto refreshToken(TokenRefreshRequestDto request);

    void logout(String refreshToken);

    void logoutAllDevices(UUID userId);

    AuthResponseDto authenticateWithOAuth2(String provider, Map<String, Object> attributes);

    void verifyEmail(String activationKey);

    void requestPasswordReset(String email);

    void resetPassword(String resetKey, String newPassword);

    User getCurrentUser();

    UUID getCurrentUserId();
}
