package com.swiftchat.auth_service.repository.impl;

import com.swiftchat.shared.security.model.User;
import com.swiftchat.auth_service.repository.CustomUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<User> findRecentlyCreatedUsers(LocalDateTime since, int limit) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.createdAt > :since ORDER BY u.createdAt DESC",
                User.class);
        query.setParameter("since", since);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public List<User> findInactiveUsersSince(LocalDateTime lastActiveDate) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.lastLoginAt < :lastActiveDate OR u.lastLoginAt IS NULL",
                User.class);
        query.setParameter("lastActiveDate", lastActiveDate);
        return query.getResultList();
    }

    @Override
    public long countActiveUsersBetween(LocalDateTime startDate, LocalDateTime endDate) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate",
                Long.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getSingleResult();
    }
}
