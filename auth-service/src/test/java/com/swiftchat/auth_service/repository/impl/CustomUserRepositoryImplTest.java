package com.swiftchat.auth_service.repository.impl;

import com.swiftchat.auth_service.repository.UserRepository;
import com.swiftchat.shared.security.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Repository Implementation Tests")
class CustomUserRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserRepositoryImpl customUserRepository;

    private User testUser1;
    private User testUser2;
    private LocalDateTime now;
    private List<User> userList;

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

        userList = Arrays.asList(testUser1, testUser2);
    }

    @Test
    @DisplayName("Should find recently created users")
    void findRecentlyCreatedUsers_ShouldReturnUsersList() {
        // Arrange
        List<User> expectedUsers = Arrays.asList(testUser1, testUser2);
        LocalDateTime since = now.minusDays(10);
        int limit = 10;

        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(mockQuery.getResultList()).thenReturn(expectedUsers);
        when(mockQuery.setParameter(eq("since"), any())).thenReturn(mockQuery);
        when(mockQuery.setMaxResults(limit)).thenReturn(mockQuery);

        when(entityManager.createQuery(
                "SELECT u FROM User u WHERE u.createdAt > :since ORDER BY u.createdAt DESC",
                User.class)).thenReturn(mockQuery);

        // Act
        List<User> result = customUserRepository.findRecentlyCreatedUsers(since, limit);

        // Assert
        assertEquals(expectedUsers, result);

        // Verify setMaxResults was actually called
        verify(mockQuery).setMaxResults(limit);
    }

    @Test
    @DisplayName("Should find inactive users since specified date")
    void findInactiveUsersSince_ShouldReturnUsersList() {
        // Arrange
        List<User> expectedUsers = Arrays.asList(testUser2);
        LocalDateTime lastActiveDate = now.minusDays(2);

        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(mockQuery.getResultList()).thenReturn(expectedUsers);
        when(mockQuery.setParameter(eq("lastActiveDate"), any())).thenReturn(mockQuery);

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

        @SuppressWarnings("unchecked")
        TypedQuery<Long> mockQuery = mock(TypedQuery.class);
        when(mockQuery.getSingleResult()).thenReturn(expectedCount);
        when(mockQuery.setParameter(eq("startDate"), any())).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("endDate"), any())).thenReturn(mockQuery);

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

        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(mockQuery.getResultList()).thenReturn(emptyList);
        when(mockQuery.setParameter(eq("since"), any())).thenReturn(mockQuery);
        when(mockQuery.setMaxResults(limit)).thenReturn(mockQuery);

        when(entityManager.createQuery(
                "SELECT u FROM User u WHERE u.createdAt > :since ORDER BY u.createdAt DESC",
                User.class)).thenReturn(mockQuery);

        // Act
        List<User> result = customUserRepository.findRecentlyCreatedUsers(since, limit);

        // Assert
        assertEquals(emptyList, result);

        // Verify setMaxResults was actually called
        verify(mockQuery).setMaxResults(limit);
    }

    @Test
    @DisplayName("Should return empty list when no inactive users")
    void findInactiveUsersSince_NoUsers_ShouldReturnEmptyList() {
        // Arrange
        List<User> emptyList = List.of();
        LocalDateTime lastActiveDate = now.minusDays(30);

        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(mockQuery.getResultList()).thenReturn(emptyList);
        when(mockQuery.setParameter(eq("lastActiveDate"), any())).thenReturn(mockQuery);

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

        @SuppressWarnings("unchecked")
        TypedQuery<Long> mockQuery = mock(TypedQuery.class);
        when(mockQuery.getSingleResult()).thenReturn(expectedCount);
        when(mockQuery.setParameter(eq("startDate"), any())).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("endDate"), any())).thenReturn(mockQuery);

        when(entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate",
                Long.class)).thenReturn(mockQuery);

        // Act
        long result = customUserRepository.countActiveUsersBetween(startDate, endDate);

        // Assert
        assertEquals(expectedCount, result);
    }

    @Test
    @DisplayName("Should find users by username or email containing search term")
    void findByUsernameOrEmailContaining_ShouldReturnMatchingUsers() {
        // Arrange
        String searchTerm = "test";
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> expectedPage = new PageImpl<>(userList, pageable, userList.size());

        // Only set up stubs needed for this test
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // Act
        Page<User> result = customUserRepository.findByUsernameOrEmailContaining(searchTerm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(testUser1, result.getContent().get(0));
        assertEquals(testUser2, result.getContent().get(1));

        // Verify interactions
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should find users by username containing search term")
    void findByUsernameContaining_ShouldReturnMatchingUsers() {
        // Arrange
        String searchTerm = "user";
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> expectedPage = new PageImpl<>(userList, pageable, userList.size());

        // Only set up stubs needed for this test
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // Act
        Page<User> result = customUserRepository.findByUsernameContaining(searchTerm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(testUser1, result.getContent().get(0));
        assertEquals(testUser2, result.getContent().get(1));

        // Verify interactions
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should find user by exact email")
    void findByExactEmail_ShouldReturnUser() {
        // Arrange
        String email = "test1@example.com";

        // Use the findOne method from JpaSpecificationExecutor
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(testUser1));

        // Act
        Optional<User> result = customUserRepository.findByExactEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser1, result.get());

        // Verify interactions
        verify(userRepository).findOne(any(Specification.class));
    }

    @Test
    @DisplayName("Should return empty when user not found by exact email")
    void findByExactEmail_WhenUserNotFound_ShouldReturnEmpty() {
        // Arrange
        String email = "nonexistent@example.com";

        // Only set up stubs needed for this test
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        // Act
        Optional<User> result = customUserRepository.findByExactEmail(email);

        // Assert
        assertFalse(result.isPresent());

        // Verify interactions
        verify(userRepository).findOne(any(Specification.class));
    }

    @Test
    @DisplayName("Should find user by exact username")
    void findByExactUsername_ShouldReturnUser() {
        // Arrange
        String username = "testuser1";

        // Use the findOne method from JpaSpecificationExecutor
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(testUser1));

        // Act
        Optional<User> result = customUserRepository.findByExactUsername(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser1, result.get());

        // Verify interactions
        verify(userRepository).findOne(any(Specification.class));
    }

    @Test
    @DisplayName("Should return empty when user not found by exact username")
    void findByExactUsername_WhenUserNotFound_ShouldReturnEmpty() {
        // Arrange
        String username = "nonexistent";

        // Only set up stubs needed for this test
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        // Act
        Optional<User> result = customUserRepository.findByExactUsername(username);

        // Assert
        assertFalse(result.isPresent());

        // Verify interactions
        verify(userRepository).findOne(any(Specification.class));
    }
}
