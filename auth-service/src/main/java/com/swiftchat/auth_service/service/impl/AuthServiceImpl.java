package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.dto.*;
import com.swiftchat.auth_service.exception.InvalidCredentialsException;
import com.swiftchat.auth_service.exception.InvalidTokenException;
import com.swiftchat.auth_service.exception.ResourceNotFoundException;
import com.swiftchat.auth_service.exception.UserNotActivatedException;
import com.swiftchat.auth_service.model.OAuth2Account;
import com.swiftchat.auth_service.model.RefreshToken;
import com.swiftchat.auth_service.model.RoleName;
import com.swiftchat.auth_service.model.User;
import com.swiftchat.auth_service.repository.OAuth2AccountRepository;
import com.swiftchat.auth_service.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2AccountRepository oAuth2AccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerRequest) {
        // Create user without roles first
        User user = userService.createUser(UserCreateDto.builder()
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build());

        // Role assignment is handled within UserService.createUser(), no need to do it
        // again here

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtService.generateToken(userDetails, user.getId());

        // Create refresh token (in a separate transaction to avoid conflicts with
        // user_roles)
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponseDto.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    // If you have a method that directly assigns roles, modify it like this:
    private void assignRolesToUser(User user) {
        // Instead of directly modifying the user.getRoles() collection or inserting
        // into user_roles table,
        // use the UserService method that now has proper duplicate checking
        userService.addRoleToUser(user.getId(), RoleName.ROLE_USER.name());
    }

    // For the createRefreshToken method, use a new transaction to prevent it from
    // being
    // rolled back if there are issues with role assignment
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    protected RefreshToken createRefreshToken(User user) {
        return refreshTokenService.createRefreshToken(user);
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
        log.info("Authenticating with OAuth2, provider: {}", provider);
        log.debug("OAuth2 user attributes: {}", attributes);

        String email = null;
        String name = null;
        String providerId = null;

        if ("google".equalsIgnoreCase(provider)) {
            // Handle Google OAuth2 user info format
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("sub");

            log.info("Google OAuth2 user info - email: {}, name: {}, sub: {}", email, name, providerId);
        } else if ("facebook".equalsIgnoreCase(provider)) {
            // Handle Facebook OAuth2 user info format
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("id");

            log.info("Facebook OAuth2 user info - email: {}, name: {}, id: {}", email, name, providerId);
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        }

        // Validate required fields
        if (email == null || name == null || providerId == null) {
            log.error("Missing required OAuth2 user info fields. Email: {}, Name: {}, ID: {}", email, name, providerId);
            throw new InvalidCredentialsException("Invalid OAuth2 credentials");
        }

        // Check for existing OAuth2 account
        Optional<OAuth2Account> existingOAuth2Account = oAuth2AccountRepository.findByProviderAndProviderId(provider,
                providerId);

        if (existingOAuth2Account.isPresent()) {
            // User already exists with this OAuth2 account
            User user = existingOAuth2Account.get().getUser();

            // Generate tokens
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String accessToken = jwtService.generateToken(userDetails, user.getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return new AuthResponseDto(accessToken, refreshToken.getToken(), "Bearer", user.getId(), user.getEmail());
        }

        // Check if user exists with the email
        Optional<User> existingUser = userService.getUserByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            // User exists, link the OAuth2 account to the existing user
            user = existingUser.get();

            // Create and link OAuth2 account
            OAuth2Account oauth2Account = new OAuth2Account();
            oauth2Account.setUser(user);
            oauth2Account.setProvider(provider);
            oauth2Account.setProviderId(providerId);
            oAuth2AccountRepository.save(oauth2Account);
        } else {
            // Create new user with OAuth2 account
            UserCreateDto userCreateDto = UserCreateDto.builder()
                    .email(email)
                    // Generate random password as user will login via OAuth
                    .password(UUID.randomUUID().toString())
                    .build();

            user = userService.createUser(userCreateDto);

            // Mark as activated since OAuth providers typically verify emails
            if (!user.isActivated()) {
                user.setActivated(true);
                user = userRepository.save(user);
            }

            // Create and link OAuth2 account
            OAuth2Account oauth2Account = new OAuth2Account();
            oauth2Account.setUser(user);
            oauth2Account.setProvider(provider);
            oauth2Account.setProviderId(providerId);
            oAuth2AccountRepository.save(oauth2Account);
        }

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails, user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponseDto(accessToken, refreshToken.getToken(), "Bearer", user.getId(), user.getEmail());
    }

    /**
     * Generate a username from the user's name
     */
    private String generateUsernameFromName(String name) {
        // Convert name to lowercase and remove spaces
        String baseName = name.toLowerCase().replaceAll("\\s+", "");

        // Try the name as is
        if (!userRepository.existsByEmail(baseName + "@example.com")) { // Using existsByEmail as proxy check
            return baseName;
        }

        // If already exists, append a random number
        return baseName + ThreadLocalRandom.current().nextInt(1000, 10000);
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
