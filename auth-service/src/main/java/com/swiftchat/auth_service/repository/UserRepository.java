package com.swiftchat.auth_service.repository;

import com.swiftchat.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByActivationKey(String activationKey);

    Optional<User> findByResetKey(String resetKey);

    @Modifying
    @Query("UPDATE User u SET u.activated = true, u.activationKey = null WHERE u.id = :id")
    void activateUser(UUID id);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :id")
    void updateLastLogin(UUID id, LocalDateTime loginTime);

    @Query(value = "SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE u.id = :userId AND r.name = :roleName")
    boolean hasRole(UUID userId, String roleName);
}
