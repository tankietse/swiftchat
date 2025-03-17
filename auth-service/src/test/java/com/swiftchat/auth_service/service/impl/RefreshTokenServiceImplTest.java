package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.exception.InvalidTokenException;
import com.swiftchat.auth_service.model.RefreshToken;
import com.swiftchat.auth_service.model.User;
import com.swiftchat.auth_service.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Refresh Token Service Implementation Tests")
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private RefreshToken validRefreshToken;
    private RefreshToken expiredRefreshToken;
    private RefreshToken revokedRefreshToken;

    @BeforeEach
    void setUp() {
        // Set the refresh token expiration time (in seconds)
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationInSeconds", 86400L);

        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .activated(true)
                .createdAt(LocalDateTime.now())
                .roles(new HashSet<>())
                .build();

        // Create a valid refresh token
        validRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("valid-refresh-token")
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        // Create an expired refresh token
        expiredRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("expired-refresh-token")
                .expiryDate(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        // Create a revoked refresh token
        revokedRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("revoked-refresh-token")
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
    }

    @Nested
    @DisplayName("Create Refresh Token Tests")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Should create and return a new refresh token")
        void createRefreshToken_ShouldCreateAndReturnNewToken() {
            // Arrange
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            RefreshToken result = refreshTokenService.createRefreshToken(testUser);

            // Assert
            assertNotNull(result);
            assertEquals(testUser, result.getUser());
            assertFalse(result.isRevoked());
            assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now()));

            // Verify the token was saved
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("Find Token Tests")
    class FindTokenTests {

        @Test
        @DisplayName("Should return refresh token when it exists")
        void findByToken_TokenExists_ShouldReturnToken() {
            // Arrange
            String tokenValue = "existing-token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validRefreshToken));

            // Act
            Optional<RefreshToken> result = refreshTokenService.findByToken(tokenValue);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(validRefreshToken, result.get());
            verify(refreshTokenRepository).findByToken(tokenValue);
        }

        @Test
        @DisplayName("Should return empty Optional when token doesn't exist")
        void findByToken_TokenDoesNotExist_ShouldReturnEmptyOptional() {
            // Arrange
            String tokenValue = "non-existent-token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // Act
            Optional<RefreshToken> result = refreshTokenService.findByToken(tokenValue);

            // Assert
            assertTrue(result.isEmpty());
            verify(refreshTokenRepository).findByToken(tokenValue);
        }
    }

    @Nested
    @DisplayName("Verify Token Tests")
    class VerifyTokenTests {

        @Test
        @DisplayName("Should return token when it's valid")
        void verifyExpiration_ValidToken_ShouldReturnToken() {
            // Act
            RefreshToken result = refreshTokenService.verifyExpiration(validRefreshToken);

            // Assert
            assertNotNull(result);
            assertEquals(validRefreshToken, result);
            verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token is expired")
        void verifyExpiration_ExpiredToken_ShouldThrowInvalidTokenException() {
            // Act & Assert
            InvalidTokenException exception = assertThrows(InvalidTokenException.class,
                    () -> refreshTokenService.verifyExpiration(expiredRefreshToken));
            assertEquals("Refresh token expired. Please log in again.", exception.getMessage());

            // Verify the expired token was deleted
            verify(refreshTokenRepository).delete(expiredRefreshToken);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token is revoked")
        void verifyExpiration_RevokedToken_ShouldThrowInvalidTokenException() {
            // Act & Assert
            InvalidTokenException exception = assertThrows(InvalidTokenException.class,
                    () -> refreshTokenService.verifyExpiration(revokedRefreshToken));
            assertEquals("Refresh token was revoked. Please log in again.", exception.getMessage());

            // Verify the revoked token was NOT deleted (just remains revoked)
            verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("Revoke Token Tests")
    class RevokeTokenTests {

        @Test
        @DisplayName("Should revoke token when it exists")
        void revokeToken_TokenExists_ShouldRevokeToken() {
            // Arrange
            String tokenValue = "token-to-revoke";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validRefreshToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validRefreshToken);

            // Act
            refreshTokenService.revokeToken(tokenValue);

            // Assert
            assertTrue(validRefreshToken.isRevoked());
            verify(refreshTokenRepository).save(validRefreshToken);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token doesn't exist")
        void revokeToken_TokenDoesNotExist_ShouldThrowInvalidTokenException() {
            // Arrange
            String tokenValue = "non-existent-token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidTokenException.class, () -> refreshTokenService.revokeToken(tokenValue));
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("Revoke All User Tokens Tests")
    class RevokeAllUserTokensTests {

        @Test
        @DisplayName("Should revoke all tokens of a user")
        void revokeAllUserTokens_ShouldCallRepository() {
            // Arrange
            UUID userId = testUser.getId();

            // Act
            refreshTokenService.revokeAllUserTokens(testUser);

            // Assert
            verify(refreshTokenRepository).revokeAllUserTokens(userId);
        }
    }

    @Nested
    @DisplayName("Find All User Tokens Tests")
    class FindAllUserTokensTests {

        @Test
        @DisplayName("Should return all tokens of a user")
        void findAllUserTokens_ShouldReturnAllTokens() {
            // Arrange
            List<RefreshToken> expectedTokens = Arrays.asList(validRefreshToken, revokedRefreshToken);
            when(refreshTokenRepository.findAllByUser(testUser)).thenReturn(expectedTokens);

            // Act
            List<RefreshToken> result = refreshTokenService.findAllUserTokens(testUser);

            // Assert
            assertEquals(expectedTokens, result);
            verify(refreshTokenRepository).findAllByUser(testUser);
        }
    }

    @Nested
    @DisplayName("Delete Expired Tokens Tests")
    class DeleteExpiredTokensTests {

        @Test
        @DisplayName("Should delete all expired tokens")
        void deleteExpiredTokens_ShouldDeleteAllExpiredTokens() {
            // Arrange
            List<RefreshToken> expiredTokens = Arrays.asList(expiredRefreshToken);
            when(refreshTokenRepository.findAllExpiredTokens(any(LocalDateTime.class))).thenReturn(expiredTokens);

            // Act
            refreshTokenService.deleteExpiredTokens();

            // Assert
            verify(refreshTokenRepository).deleteAllExpiredTokens(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should handle exceptions when deleting expired tokens")
        void deleteExpiredTokens_Exception_ShouldHandleGracefully() {
            // Arrange
            when(refreshTokenRepository.findAllExpiredTokens(any(LocalDateTime.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert (should not throw exception)
            assertDoesNotThrow(() -> refreshTokenService.deleteExpiredTokens());
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return true for a valid token")
        void isTokenValid_ValidToken_ShouldReturnTrue() {
            // Arrange
            String tokenValue = "valid-token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validRefreshToken));

            // Act
            boolean result = refreshTokenService.isTokenValid(tokenValue);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false for an expired token")
        void isTokenValid_ExpiredToken_ShouldReturnFalse() {
            // Arrange
            String tokenValue = "expired-token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredRefreshToken));

            // Act
            boolean result = refreshTokenService.isTokenValid(tokenValue);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for a revoked token")
        void isTokenValid_RevokedToken_ShouldReturnFalse() {
            // Arrange
            String tokenValue = "revoked-token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(revokedRefreshToken));

            // Act
            boolean result = refreshTokenService.isTokenValid(tokenValue);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for a non-existent token")
        void isTokenValid_NonExistentToken_ShouldReturnFalse() {
            // Arrange
            String tokenValue = "non-existent-token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // Act
            boolean result = refreshTokenService.isTokenValid(tokenValue);

            // Assert
            assertFalse(result);
        }
    }

    @Test
    @DisplayName("Should count active tokens")
    void countActiveTokens_ShouldReturnCount() {
        // Arrange
        long expectedCount = 5;
        when(refreshTokenRepository.countByUserAndRevokedFalseAndExpiryDateAfter(eq(testUser),
                any(LocalDateTime.class)))
                .thenReturn(expectedCount);

        // Act
        long result = refreshTokenService.countActiveTokens(testUser);

        // Assert
        assertEquals(expectedCount, result);
        verify(refreshTokenRepository).countByUserAndRevokedFalseAndExpiryDateAfter(eq(testUser),
                any(LocalDateTime.class));
    }
}
