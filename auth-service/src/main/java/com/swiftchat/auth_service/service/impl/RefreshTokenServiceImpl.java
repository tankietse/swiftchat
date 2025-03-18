package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.exception.InvalidTokenException;
import com.swiftchat.auth_service.model.RefreshToken;
import com.swiftchat.shared.security.model.User;
import com.swiftchat.auth_service.repository.RefreshTokenRepository;
import com.swiftchat.auth_service.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenDurationInSeconds;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationInSeconds))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token expired. Please log in again.");
        }

        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token was revoked. Please log in again.");
        }

        return token;
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user.getId());
    }

    @Override
    public List<RefreshToken> findAllUserTokens(User user) {
        return refreshTokenRepository.findAllByUser(user);
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 86400000) // Run daily
    public void deleteExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<RefreshToken> expiredTokens = refreshTokenRepository.findAllExpiredTokens(now);
            log.info("Found {} expired refresh tokens to delete", expiredTokens.size());
            refreshTokenRepository.deleteAllExpiredTokens(now);
        } catch (Exception e) {
            log.error("Error while deleting expired tokens", e);
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        return findByToken(token)
                .map(t -> !t.isExpired() && !t.isRevoked())
                .orElse(false);
    }

    @Override
    public long countActiveTokens(User user) {
        return refreshTokenRepository.countByUserAndRevokedFalseAndExpiryDateAfter(user, LocalDateTime.now());
    }
}
