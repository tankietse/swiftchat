package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.dto.*;
import com.swiftchat.auth_service.exception.InvalidCredentialsException;
import com.swiftchat.auth_service.exception.InvalidTokenException;
import com.swiftchat.auth_service.exception.ResourceNotFoundException;
import com.swiftchat.auth_service.exception.UserNotActivatedException;
import com.swiftchat.auth_service.model.OAuth2Account;
import com.swiftchat.auth_service.model.RefreshToken;
import com.swiftchat.auth_service.model.User;
import com.swiftchat.auth_service.repository.OAuth2AccountRepository;
import com.swiftchat.auth_service.security.JwtService;
import com.swiftchat.auth_service.service.AuthService;
import com.swiftchat.auth_service.service.RefreshTokenService;
import com.swiftchat.auth_service.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2AccountRepository oAuth2AccountRepository;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        UserCreateDto userCreateDto = UserCreateDto.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        User user = userService.createUser(userCreateDto);

        // In a real-world scenario, the user would need to verify their email
        // before they could log in. For demonstration purposes, we're allowing
        // immediate login after registration.

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtService.generateToken(userDetails, user.getId());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        userService.updateLastLogin(user.getId(), LocalDateTime.now());

        return AuthResponseDto.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get user details
        User user = userService.getUserByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Check if user is activated
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User account is not activated");
        }

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails, user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Update last login time
        userService.updateLastLogin(user.getId(), LocalDateTime.now());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDto refreshToken(TokenRefreshRequestDto request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Revoke the used refresh token and create a new one
        refreshTokenService.revokeToken(requestRefreshToken);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        // Generate a new access token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails, user.getId());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        refreshTokenService.revokeAllUserTokens(user);
    }

    @Override
    @Transactional
    public AuthResponseDto authenticateWithOAuth2(String provider, Map<String, Object> attributes) {
        String providerId = (String) attributes.get("id");
        String email = (String) attributes.get("email");

        if (providerId == null || email == null) {
            throw new InvalidCredentialsException("Invalid OAuth2 credentials");
        }

        // Check if OAuth2 account exists
        Optional<OAuth2Account> existingOAuth2Account = oAuth2AccountRepository.findByProviderAndProviderId(provider,
                providerId);

        User user;
        if (existingOAuth2Account.isPresent()) {
            // If OAuth2 account exists, get the user
            user = existingOAuth2Account.get().getUser();
        } else {
            // If not, check if user with the email exists
            Optional<User> existingUser = userService.getUserByEmail(email);

            if (existingUser.isPresent()) {
                // Link OAuth2 account to existing user
                user = existingUser.get();
                OAuth2Account oauth2Account = OAuth2Account.builder()
                        .user(user)
                        .provider(provider)
                        .providerId(providerId)
                        .build();
                oAuth2AccountRepository.save(oauth2Account);
            } else {
                // Create a new user and OAuth2 account
                UserCreateDto userCreateDto = UserCreateDto.builder()
                        .email(email)
                        .password(UUID.randomUUID().toString()) // Generate a random password
                        .build();

                user = userService.createUser(userCreateDto);

                // Set as activated since OAuth2 users are pre-verified
                user.setActivated(true);

                OAuth2Account oauth2Account = OAuth2Account.builder()
                        .user(user)
                        .provider(provider)
                        .providerId(providerId)
                        .build();
                oAuth2AccountRepository.save(oauth2Account);
            }
        }

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails, user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Update last login time
        userService.updateLastLogin(user.getId(), LocalDateTime.now());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String activationKey) {
        userService.activateUser(activationKey);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        userService.requestPasswordReset(email);
    }

    @Override
    @Transactional
    public void resetPassword(String resetKey, String newPassword) {
        userService.completePasswordReset(resetKey, newPassword);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new InvalidCredentialsException("User not authenticated");
        }

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userService.getUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found with email: " + email));
    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
