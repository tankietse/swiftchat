package com.swiftchat.auth_service.repository.impl;

import com.swiftchat.shared.security.model.User;
import com.swiftchat.auth_service.repository.CustomUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final JpaRepository<User, UUID> userRepository;
    private final JpaSpecificationExecutor<User> userSpecificationExecutor;

    public CustomUserRepositoryImpl(EntityManager entityManager,
            JpaRepository<User, UUID> userRepository,
            JpaSpecificationExecutor<User> userSpecificationExecutor) {
        this.entityManager = entityManager;
        this.userRepository = userRepository;
        this.userSpecificationExecutor = userSpecificationExecutor;
    }

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

    /**
     * Find users whose username or email contains the search term
     * 
     * @param searchTerm the term to search for in username or email
     * @param pageable   pagination information
     * @return page of matching users
     */
    public Page<User> findByUsernameOrEmailContaining(String searchTerm, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            String likePattern = "%" + searchTerm + "%";
            return cb.or(
                    cb.like(root.get("username"), likePattern),
                    cb.like(root.get("email"), likePattern));
        };

        return userSpecificationExecutor.findAll(spec, pageable);
    }

    /**
     * Find users whose username contains the search term
     * 
     * @param searchTerm the term to search for in username
     * @param pageable   pagination information
     * @return page of matching users
     */
    public Page<User> findByUsernameContaining(String searchTerm, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            String likePattern = "%" + searchTerm + "%";
            return cb.like(root.get("username"), likePattern);
        };

        return userSpecificationExecutor.findAll(spec, pageable);
    }

    /**
     * Find a user by their exact email address
     * 
     * @param email the exact email to search for
     * @return optional containing the user if found
     */
    public Optional<User> findByExactEmail(String email) {
        Specification<User> spec = (root, query, cb) -> cb.equal(root.get("email"), email);

        return userSpecificationExecutor.findOne(spec);
    }

    /**
     * Find a user by their exact username
     * 
     * @param username the exact username to search for
     * @return optional containing the user if found
     */
    public Optional<User> findByExactUsername(String username) {
        Specification<User> spec = (root, query, cb) -> cb.equal(root.get("username"), username);

        return userSpecificationExecutor.findOne(spec);
    }
}
