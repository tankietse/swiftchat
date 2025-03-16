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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication API for user registration, login, and token management")
public class AuthController {

        private final AuthService authService;

        @Value("${spring.security.oauth2.client.registration.google.client-id:}")
        private String googleClientId;

        @Value("${spring.security.oauth2.client.registration.facebook.client-id:}")
        private String facebookClientId;

        @Value("${app.frontend-url}")
        private String frontendUrl;

        @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
        private String googleClientSecret;

        @Value("${spring.security.oauth2.client.registration.facebook.client-secret:}")
        private String facebookClientSecret;

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

        @GetMapping("/oauth2/{provider}")
        @Operation(summary = "OAuth2 authorization", description = "Redirects user to the OAuth2 provider's authorization page")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "302", description = "Redirect to OAuth2 provider"),
                        @ApiResponse(responseCode = "400", description = "Invalid OAuth provider")
        })
        public void oauth2Authorize(
                        @PathVariable @Parameter(description = "OAuth2 provider (google, facebook)", required = true) String provider,
                        HttpServletResponse response) throws IOException {

                log.info("Initiating OAuth2 authorization for provider: {}", provider);

                String authorizationUrl;

                if ("google".equalsIgnoreCase(provider)) {
                        authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                                        "?client_id=" + googleClientId +
                                        "&response_type=code" +
                                        "&scope=email%20profile" +
                                        "&redirect_uri="
                                        + URLEncoder.encode(frontendUrl + "/oauth2/callback/google",
                                                        StandardCharsets.UTF_8)
                                        +
                                        "&state=" + generateSecureState();
                } else if ("facebook".equalsIgnoreCase(provider)) {
                        authorizationUrl = "https://www.facebook.com/v12.0/dialog/oauth" +
                                        "?client_id=" + facebookClientId +
                                        "&response_type=code" +
                                        "&scope=email,public_profile" +
                                        "&redirect_uri="
                                        + URLEncoder.encode(frontendUrl + "/oauth2/callback/facebook",
                                                        StandardCharsets.UTF_8)
                                        +
                                        "&state=" + generateSecureState();
                } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported OAuth2 provider");
                        return;
                }

                // Redirect to the authorization URL
                response.sendRedirect(authorizationUrl);
        }

        @PostMapping("/oauth2/{provider}/callback")
        @Operation(summary = "OAuth2 callback handler", description = "Processes the OAuth2 authorization code and authenticates the user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid OAuth provider or authorization code")
        })
        public ResponseEntity<AuthResponseDto> oauth2Callback(
                        @PathVariable @Parameter(description = "OAuth2 provider (google, facebook)", required = true) String provider,
                        @RequestParam(required = false) @Parameter(description = "Authorization code from OAuth2 provider") String code,
                        @RequestBody(required = false) Map<String, Object> requestBody) {

                log.info("Received OAuth2 callback for provider: {} with request parameters: {}", provider,
                                code != null ? "code present" : "code missing");
                if (requestBody != null) {
                        log.info("Request body: {}", requestBody);
                }

                try {
                        // Try to get code from request body if not in request params
                        String authCode = code;
                        if (authCode == null && requestBody != null && requestBody.containsKey("code")) {
                                authCode = requestBody.get("code").toString();
                                log.info("Using code from request body");
                        }

                        if (authCode == null) {
                                log.error("Authorization code is missing from both request parameters and request body");
                                throw new IllegalArgumentException("Authorization code is required");
                        }

                        // Exchange the authorization code for tokens and user information
                        Map<String, Object> userData = exchangeAuthorizationCode(provider, authCode);

                        // Authenticate the user with the obtained data
                        AuthResponseDto authResponse = authService.authenticateWithOAuth2(provider, userData);
                        return ResponseEntity.ok(authResponse);
                } catch (Exception e) {
                        log.error("Failed to process OAuth2 callback: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to authenticate with " + provider + ": " + e.getMessage(),
                                        e);
                }
        }

        private Map<String, Object> exchangeAuthorizationCode(String provider, String code) {
                String tokenUrl;
                String clientId;
                String clientSecret;
                String redirectUri = frontendUrl + "/oauth2/callback/" + provider;

                // Set provider-specific details
                if ("google".equalsIgnoreCase(provider)) {
                        tokenUrl = "https://oauth2.googleapis.com/token";
                        clientId = googleClientId;
                        clientSecret = googleClientSecret;
                } else if ("facebook".equalsIgnoreCase(provider)) {
                        tokenUrl = "https://graph.facebook.com/v12.0/oauth/access_token";
                        clientId = facebookClientId;
                        clientSecret = facebookClientSecret;
                } else {
                        throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
                }

                // Prepare the token request
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
                requestBody.add("code", code);
                requestBody.add("client_id", clientId);
                requestBody.add("client_secret", clientSecret);
                requestBody.add("redirect_uri", redirectUri);
                requestBody.add("grant_type", "authorization_code");

                HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

                // Exchange code for access token
                Map<String, Object> tokenResponse = restTemplate.exchange(
                                tokenUrl,
                                HttpMethod.POST,
                                requestEntity,
                                Map.class).getBody();

                // Get user info with the access token
                String accessToken = (String) tokenResponse.get("access_token");
                String userInfoUrl;

                if ("google".equalsIgnoreCase(provider)) {
                        userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
                } else { // facebook
                        userInfoUrl = "https://graph.facebook.com/me?fields=id,name,email,picture&access_token="
                                        + accessToken;
                }

                HttpHeaders userInfoHeaders = new HttpHeaders();
                if ("google".equalsIgnoreCase(provider)) {
                        userInfoHeaders.setBearerAuth(accessToken);
                }

                HttpEntity<?> userInfoRequest = new HttpEntity<>(userInfoHeaders);

                // Get user information
                Map<String, Object> userInfo = restTemplate.exchange(
                                userInfoUrl,
                                HttpMethod.GET,
                                userInfoRequest,
                                Map.class).getBody();

                return userInfo;
        }

        // Helper method to generate a secure state parameter for OAuth2 flow
        private String generateSecureState() {
                byte[] bytes = new byte[16];
                new SecureRandom().nextBytes(bytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
