package com.swiftchat.auth_service.repository.impl;

import com.swiftchat.auth_service.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Repository Implementation Tests")
class CustomUserRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CustomUserRepositoryImpl customUserRepository;

    private User testUser1;
    private User testUser2;
    private LocalDateTime now;

    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> mockTypedQuery(List<T> resultList) {
        TypedQuery<T> query = mock(TypedQuery.class);
        when(query.getResultList()).thenReturn(resultList);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(resultList.isEmpty() ? null : resultList.get(0));
        return query;
    }

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        testUser1 = User.builder()
                .id(UUID.randomUUID())
                .email("user1@example.com")
                .passwordHash("hashedPassword1")
                .activated(true)
                .createdAt(now.minusDays(5))
                .lastLoginAt(now.minusDays(1))
                .build();

        testUser2 = User.builder()
                .id(UUID.randomUUID())
                .email("user2@example.com")
                .passwordHash("hashedPassword2")
                .activated(true)
                .createdAt(now.minusDays(10))
                .lastLoginAt(now.minusDays(3))
                .build();
    }

    @Test
    @DisplayName("Should find recently created users")
    void findRecentlyCreatedUsers_ShouldReturnUsersList() {
        // Arrange
        List<User> expectedUsers = Arrays.asList(testUser1, testUser2);
        LocalDateTime since = now.minusDays(10);
        int limit = 10;

        TypedQuery<User> mockQuery = mockTypedQuery(expectedUsers);

        when(entityManager.createQuery(
                "SELECT u FROM User u WHERE u.createdAt > :since ORDER BY u.createdAt DESC",
                User.class)).thenReturn(mockQuery);

        // Act
        List<User> result = customUserRepository.findRecentlyCreatedUsers(since, limit);

        // Assert
        assertEquals(expectedUsers, result);
    }

    @Test
    @DisplayName("Should find inactive users since specified date")
    void findInactiveUsersSince_ShouldReturnUsersList() {
        // Arrange
        List<User> expectedUsers = Arrays.asList(testUser2);
        LocalDateTime lastActiveDate = now.minusDays(2);

        TypedQuery<User> mockQuery = mockTypedQuery(expectedUsers);

        when(entityManager.createQuery(
                "SELECT u FROM User u WHERE u.lastLoginAt < :lastActiveDate OR u.lastLoginAt IS NULL",
                User.class)).thenReturn(mockQuery);

        // Act
        List<User> result = customUserRepository.findInactiveUsersSince(lastActiveDate);

        // Assert
        assertEquals(expectedUsers, result);
    }

    @Test
    @DisplayName("Should count active users between dates")
    void countActiveUsersBetween_ShouldReturnCount() {
        // Arrange
        long expectedCount = 2L;
        LocalDateTime startDate = now.minusDays(7);
        LocalDateTime endDate = now;

        TypedQuery<Long> mockQuery = mockTypedQuery(Arrays.asList(expectedCount));

        when(entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate",
                Long.class)).thenReturn(mockQuery);

        // Act
        long result = customUserRepository.countActiveUsersBetween(startDate, endDate);

        // Assert
        assertEquals(expectedCount, result);
    }

    @Test
    @DisplayName("Should return empty list when no recently created users")
    void findRecentlyCreatedUsers_NoUsers_ShouldReturnEmptyList() {
        // Arrange
        List<User> emptyList = List.of();
        LocalDateTime since = now.minusDays(1);
        int limit = 10;

        TypedQuery<User> mockQuery = mockTypedQuery(emptyList);

        when(entityManager.createQuery(
                "SELECT u FROM User u WHERE u.createdAt > :since ORDER BY u.createdAt DESC",
                User.class)).thenReturn(mockQuery);

        // Act
        List<User> result = customUserRepository.findRecentlyCreatedUsers(since, limit);

        // Assert
        assertEquals(emptyList, result);
    }

    @Test
    @DisplayName("Should return empty list when no inactive users")
    void findInactiveUsersSince_NoUsers_ShouldReturnEmptyList() {
        // Arrange
        List<User> emptyList = List.of();
        LocalDateTime lastActiveDate = now.minusDays(30);

        TypedQuery<User> mockQuery = mockTypedQuery(emptyList);

        when(entityManager.createQuery(
                "SELECT u FROM User u WHERE u.lastLoginAt < :lastActiveDate OR u.lastLoginAt IS NULL",
                User.class)).thenReturn(mockQuery);

        // Act
        List<User> result = customUserRepository.findInactiveUsersSince(lastActiveDate);

        // Assert
        assertEquals(emptyList, result);
    }

    @Test
    @DisplayName("Should return zero when no active users in date range")
    void countActiveUsersBetween_NoUsers_ShouldReturnZero() {
        // Arrange
        long expectedCount = 0L;
        LocalDateTime startDate = now.minusYears(2);
        LocalDateTime endDate = now.minusYears(1);

        TypedQuery<Long> mockQuery = mockTypedQuery(Arrays.asList(expectedCount));

        when(entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate",
                Long.class)).thenReturn(mockQuery);

        // Act
        long result = customUserRepository.countActiveUsersBetween(startDate, endDate);

        // Assert
        assertEquals(expectedCount, result);
    }
}
