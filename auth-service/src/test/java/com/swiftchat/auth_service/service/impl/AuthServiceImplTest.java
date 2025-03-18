package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.dto.*;
import com.swiftchat.auth_service.exception.InvalidCredentialsException;
import com.swiftchat.auth_service.exception.InvalidTokenException;
import com.swiftchat.auth_service.exception.ResourceNotFoundException;
import com.swiftchat.auth_service.exception.UserNotActivatedException;
import com.swiftchat.shared.security.model.OAuth2Account;
import com.swiftchat.auth_service.model.RefreshToken;
import com.swiftchat.shared.security.model.Role;
import com.swiftchat.shared.security.model.User;
import com.swiftchat.auth_service.repository.OAuth2AccountRepository;
import com.swiftchat.auth_service.repository.UserRepository;
import com.swiftchat.shared.security.jwt.JwtService;
import com.swiftchat.auth_service.service.RefreshTokenService;
import com.swiftchat.auth_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Implementation Tests")
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OAuth2AccountRepository oAuth2AccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UUID userId;
    private RefreshToken refreshToken;
    private String accessTokenString;
    private String refreshTokenString;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .activated(true)
                .createdAt(LocalDateTime.now())
                .roles(new HashSet<>())
                .build();

        refreshTokenString = "refresh-token-123";
        accessTokenString = "access-token-123";

        refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(refreshTokenString)
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusDays(1)) // 1 day in future
                .build();
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should create new user and return auth response when registering")
        void register_ShouldReturnAuthResponse() {
            // Arrange
            RegisterRequestDto registerRequest = new RegisterRequestDto("test@example.com", "password");
            UserCreateDto userCreateDto = UserCreateDto.builder()
                    .email(registerRequest.getEmail())
                    .password(registerRequest.getPassword())
                    .build();

            when(userService.createUser(any(UserCreateDto.class))).thenReturn(testUser);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn(accessTokenString);
            when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

            // Act
            AuthResponseDto response = authService.register(registerRequest);

            // Assert
            assertNotNull(response);
            assertEquals(accessTokenString, response.getAccessToken());
            assertEquals(refreshTokenString, response.getRefreshToken());
            assertEquals(userId, response.getUserId());
            assertEquals(testUser.getEmail(), response.getEmail());
            assertEquals("Bearer", response.getTokenType());

            verify(userService).createUser(any(UserCreateDto.class));
            verify(jwtService).generateToken(any(UserDetails.class), eq(userId));
            verify(refreshTokenService).createRefreshToken(testUser);
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should authenticate and return tokens when credentials are valid")
        void login_SuccessfulAuthentication_ShouldReturnAuthResponse() {
            // Arrange
            LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userService.getUserByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn(accessTokenString);
            when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

            // Act
            AuthResponseDto response = authService.login(loginRequest);

            // Assert
            assertNotNull(response);
            assertEquals(accessTokenString, response.getAccessToken());
            assertEquals(refreshTokenString, response.getRefreshToken());
            assertEquals(userId, response.getUserId());
            assertEquals(testUser.getEmail(), response.getEmail());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService).getUserByEmail(loginRequest.getEmail());
            verify(userService).updateLastLogin(eq(userId), any(LocalDateTime.class));
            verify(jwtService).generateToken(any(UserDetails.class), eq(userId));
            verify(refreshTokenService).createRefreshToken(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not found")
        void login_UserNotFound_ShouldThrowResourceNotFoundException() {
            // Arrange
            LoginRequestDto loginRequest = new LoginRequestDto("nonexistent@example.com", "password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userService.getUserByEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService).getUserByEmail(loginRequest.getEmail());
            verify(jwtService, never()).generateToken(any(UserDetails.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should throw UserNotActivatedException when user account is not activated")
        void login_UserNotActivated_ShouldThrowUserNotActivatedException() {
            // Arrange
            LoginRequestDto loginRequest = new LoginRequestDto("inactive@example.com", "password");
            testUser.setActivated(false);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userService.getUserByEmail(anyString())).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(UserNotActivatedException.class, () -> authService.login(loginRequest));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService).getUserByEmail(loginRequest.getEmail());
            verify(jwtService, never()).generateToken(any(UserDetails.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when credentials are invalid")
        void login_InvalidCredentials_ShouldThrowBadCredentialsException() {
            // Arrange
            LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // Act & Assert
            assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService, never()).getUserByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        @Test
        @DisplayName("Should return new access and refresh tokens when refresh token is valid")
        void refreshToken_ValidToken_ShouldReturnNewAuthResponse() {
            // Arrange
            TokenRefreshRequestDto refreshRequest = new TokenRefreshRequestDto(refreshTokenString);

            when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
            when(refreshTokenService.verifyExpiration(any(RefreshToken.class))).thenReturn(refreshToken);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn(accessTokenString);

            RefreshToken newRefreshToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token("new-refresh-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().plusDays(1))
                    .build();

            when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(newRefreshToken);

            // Act
            AuthResponseDto response = authService.refreshToken(refreshRequest);

            // Assert
            assertNotNull(response);
            assertEquals(accessTokenString, response.getAccessToken());
            assertEquals("new-refresh-token", response.getRefreshToken());
            assertEquals(userId, response.getUserId());

            verify(refreshTokenService).findByToken(refreshTokenString);
            verify(refreshTokenService).verifyExpiration(refreshToken);
            verify(refreshTokenService).revokeToken(refreshTokenString);
            verify(refreshTokenService).createRefreshToken(testUser);
            verify(jwtService).generateToken(any(UserDetails.class), eq(userId));
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when refresh token is not found")
        void refreshToken_TokenNotFound_ShouldThrowInvalidTokenException() {
            // Arrange
            TokenRefreshRequestDto refreshRequest = new TokenRefreshRequestDto("nonexistent-token");

            when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidTokenException.class, () -> authService.refreshToken(refreshRequest));

            verify(refreshTokenService).findByToken("nonexistent-token");
            verify(refreshTokenService, never()).verifyExpiration(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when refresh token has expired")
        void refreshToken_ExpiredToken_ShouldThrowInvalidTokenException() {
            // Arrange
            TokenRefreshRequestDto refreshRequest = new TokenRefreshRequestDto(refreshTokenString);

            when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
            when(refreshTokenService.verifyExpiration(any(RefreshToken.class)))
                    .thenThrow(new InvalidTokenException("Refresh token expired"));

            // Act & Assert
            assertThrows(InvalidTokenException.class, () -> authService.refreshToken(refreshRequest));

            verify(refreshTokenService).findByToken(refreshTokenString);
            verify(refreshTokenService).verifyExpiration(refreshToken);
            verify(refreshTokenService, never()).revokeToken(anyString());
        }

        @Test
        @DisplayName("Should revoke refresh token when logging out")
        void logout_ShouldCallRevokeToken() {
            // Act
            authService.logout(refreshTokenString);

            // Assert
            verify(refreshTokenService).revokeToken(refreshTokenString);
        }

        @Test
        @DisplayName("Should revoke all user tokens when logging out from all devices")
        void logoutAllDevices_ShouldCallRevokeAllUserTokens() {
            // Arrange
            when(userService.getUserById(any(UUID.class))).thenReturn(Optional.of(testUser));

            // Act
            authService.logoutAllDevices(userId);

            // Assert
            verify(userService).getUserById(userId);
            verify(refreshTokenService).revokeAllUserTokens(testUser);
        }
    }

    @Nested
    @DisplayName("OAuth2 Authentication Tests")
    class OAuth2AuthenticationTests {

        @ParameterizedTest
        @ValueSource(strings = { "google", "facebook" })
        @DisplayName("Should authenticate user with OAuth2 provider")
        void authenticateWithOAuth2_ExistingOAuth2Account_ShouldReturnAuthResponse(String provider) {
            // Arrange
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("email", "test@example.com");
            attributes.put("name", "Test User");
            attributes.put("sub", provider + "-123"); // Use provider in the ID

            if ("facebook".equals(provider)) {
                attributes.remove("sub");
                attributes.put("id", provider + "-123"); // Facebook uses "id" not "sub"
            }

            OAuth2Account oAuth2Account = new OAuth2Account();
            oAuth2Account.setUser(testUser);
            oAuth2Account.setProvider(provider);
            oAuth2Account.setProviderId(provider + "-123");

            when(oAuth2AccountRepository.findByProviderAndProviderId(eq(provider), anyString()))
                    .thenReturn(Optional.of(oAuth2Account));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn(accessTokenString);
            when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

            // Act
            AuthResponseDto response = authService.authenticateWithOAuth2(provider, attributes);

            // Assert
            assertNotNull(response);
            assertEquals(accessTokenString, response.getAccessToken());
            assertEquals(refreshTokenString, response.getRefreshToken());
            assertEquals(userId, response.getUserId());

            verify(oAuth2AccountRepository).findByProviderAndProviderId(eq(provider), anyString());
            verify(userDetailsService).loadUserByUsername(testUser.getEmail());
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("Should activate user account when verification token is valid")
        void verifyEmail_ShouldCallUserServiceActivateUser() {
            // Arrange
            String activationKey = "activation-key-123";

            // Act
            authService.verifyEmail(activationKey);

            // Assert
            verify(userService).activateUser(activationKey);
        }

        @Test
        @DisplayName("Should initiate password reset process when email exists")
        void requestPasswordReset_ShouldCallUserServiceRequestPasswordReset() {
            // Arrange
            String email = "test@example.com";

            // Act
            authService.requestPasswordReset(email);

            // Assert
            verify(userService).requestPasswordReset(email);
        }

        @Test
        @DisplayName("Should reset password when reset token is valid")
        void resetPassword_ShouldCallUserServiceCompletePasswordReset() {
            // Arrange
            String resetKey = "reset-key-123";
            String newPassword = "newPassword";

            // Act
            authService.resetPassword(resetKey, newPassword);

            // Assert
            verify(userService).completePasswordReset(resetKey, newPassword);
        }
    }

    @Nested
    @DisplayName("User Context Tests")
    class UserContextTests {

        @Test
        @DisplayName("Should return current authenticated user")
        void getCurrentUser_AuthenticatedUser_ShouldReturnUser() {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetails.getUsername()).thenReturn("test@example.com");
            when(userService.getUserByEmail(anyString())).thenReturn(Optional.of(testUser));

            // Act
            User user = authService.getCurrentUser();

            // Assert
            assertNotNull(user);
            assertEquals(testUser.getId(), user.getId());
            assertEquals(testUser.getEmail(), user.getEmail());

            verify(userService).getUserByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user is not authenticated")
        void getCurrentUser_NotAuthenticated_ShouldThrowInvalidCredentialsException() {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.isAuthenticated()).thenReturn(false);

            // Act & Assert
            assertThrows(InvalidCredentialsException.class, () -> authService.getCurrentUser());

            verify(userService, never()).getUserByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user is anonymous")
        void getCurrentUser_AnonymousUser_ShouldThrowInvalidCredentialsException() {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            // Act & Assert
            assertThrows(InvalidCredentialsException.class, () -> authService.getCurrentUser());

            verify(userService, never()).getUserByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not found")
        void getCurrentUser_UserNotFound_ShouldThrowResourceNotFoundException() {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetails.getUsername()).thenReturn("test@example.com");
            when(userService.getUserByEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser());

            verify(userService).getUserByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should return current authenticated user ID")
        void getCurrentUserId_ShouldReturnUserId() {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetails.getUsername()).thenReturn("test@example.com");
            when(userService.getUserByEmail(anyString())).thenReturn(Optional.of(testUser));

            // Act
            UUID result = authService.getCurrentUserId();

            // Assert
            assertEquals(userId, result);
        }
    }
}
