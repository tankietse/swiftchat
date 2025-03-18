package com.swiftchat.auth_service.repository;

import com.swiftchat.shared.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    void activateUser(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :id")
    void updateLastLogin(@Param("id") UUID id, @Param("loginTime") LocalDateTime loginTime);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE u.id = :userId AND r.name = :roleName")
    boolean hasRole(@Param("userId") UUID userId, @Param("roleName") String roleName);
}
