package com.swiftchat.auth_service.controller;

import com.swiftchat.auth_service.dto.*;
import com.swiftchat.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication API for user registration, login, and token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns authentication tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "User with this email already exists"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody @Parameter(description = "Registration details", required = true) RegisterRequestDto request) {

        log.info("User registration request for email: {}", request.getEmail());
        AuthResponseDto authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns authentication tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account not activated")
    })
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody @Parameter(description = "Login credentials", required = true) LoginRequestDto request) {

        log.info("Login attempt for user: {}", request.getEmail());
        AuthResponseDto authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Provides a new access token using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthResponseDto> refreshToken(
            @Valid @RequestBody @Parameter(description = "Refresh token details", required = true) TokenRefreshRequestDto request) {

        log.info("Token refresh request received");
        AuthResponseDto authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the user's refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logged out successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Void> logout(
            @Valid @RequestBody @Parameter(description = "Refresh token to invalidate", required = true) TokenRefreshRequestDto request) {

        log.info("Logout request received");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices", description = "Invalidates all refresh tokens for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logged out from all devices"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request")
    })
    public ResponseEntity<Void> logoutAllDevices(
            @RequestParam @Parameter(description = "User ID", required = true) UUID userId) {

        log.info("Request to logout from all devices for user: {}", userId);
        authService.logoutAllDevices(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth2/{provider}")
    @Operation(summary = "OAuth2 authentication", description = "Authenticates a user via OAuth2 provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid OAuth provider or data")
    })
    public ResponseEntity<AuthResponseDto> oauthAuthenticate(
            @PathVariable @Parameter(description = "OAuth2 provider (google, facebook)", required = true) String provider,
            @RequestBody @Parameter(description = "OAuth2 user attributes", required = true) Map<String, Object> attributes) {

        log.info("OAuth2 authentication request for provider: {}", provider);
        AuthResponseDto authResponse = authService.authenticateWithOAuth2(provider, attributes);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email", description = "Activates a user account using the email verification token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid verification token")
    })
    public ResponseEntity<MessageResponseDto> verifyEmail(
            @RequestParam @Parameter(description = "Email activation key", required = true) String token) {

        log.info("Email verification request with token: {}", token);
        authService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponseDto("Email verified successfully"));
    }

    @PostMapping("/reset-password/request")
    @Operation(summary = "Request password reset", description = "Sends a password reset link to the user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<MessageResponseDto> requestPasswordReset(
            @RequestBody @Parameter(description = "Password reset request", required = true) PasswordResetRequestDto request) {

        log.info("Password reset request for email: {}", request.getEmail());
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(new MessageResponseDto("Password reset instructions sent to email"));
    }

    @PostMapping("/reset-password/confirm")
    @Operation(summary = "Confirm password reset", description = "Resets the user password using the reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<MessageResponseDto> confirmPasswordReset(
            @Valid @RequestBody @Parameter(description = "Password reset confirmation details", required = true) PasswordResetConfirmDto request) {

        log.info("Password reset confirmation with token");
        authService.resetPassword(request.getResetToken(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponseDto("Password has been reset successfully"));
    }
}
