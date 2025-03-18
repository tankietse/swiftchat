package com.swiftchat.auth_service.service;

import com.swiftchat.auth_service.model.RefreshToken;
import com.swiftchat.shared.security.model.User;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    Optional<RefreshToken> findByToken(String token);

    RefreshToken verifyExpiration(RefreshToken token);

    void revokeToken(String token);

    void revokeAllUserTokens(User user);

    List<RefreshToken> findAllUserTokens(User user);

    void deleteExpiredTokens();

    boolean isTokenValid(String token);

    long countActiveTokens(User user);
}
