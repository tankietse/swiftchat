package com.swiftchat.auth_service.repository;

import com.swiftchat.auth_service.model.RefreshToken;
import com.swiftchat.shared.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser(User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllUserTokens(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM RefreshToken r WHERE r.expiryDate < :now AND r.revoked = false")
    List<RefreshToken> findAllExpiredTokens(@Param("now") LocalDateTime now);

    boolean existsByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user = :user")
    void deleteByUser(@Param("user") User user);

    long countByUserAndRevokedFalseAndExpiryDateAfter(User user, LocalDateTime now);
}
