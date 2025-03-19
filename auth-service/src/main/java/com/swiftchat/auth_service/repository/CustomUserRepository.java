package com.swiftchat.auth_service.repository;

import com.swiftchat.shared.security.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomUserRepository {

    List<User> findRecentlyCreatedUsers(LocalDateTime since, int limit);

    List<User> findInactiveUsersSince(LocalDateTime lastActiveDate);

    long countActiveUsersBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<User> findByUsernameOrEmailContaining(String searchTerm, Pageable pageable);

    Page<User> findByUsernameContaining(String searchTerm, Pageable pageable);

    Optional<User> findByExactEmail(String email);

    Optional<User> findByExactUsername(String username);
}
