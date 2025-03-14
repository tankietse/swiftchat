package com.swiftchat.auth_service.repository;

import com.swiftchat.auth_service.model.User;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomUserRepository {

    List<User> findRecentlyCreatedUsers(LocalDateTime since, int limit);

    List<User> findInactiveUsersSince(LocalDateTime lastActiveDate);

    long countActiveUsersBetween(LocalDateTime startDate, LocalDateTime endDate);
}
