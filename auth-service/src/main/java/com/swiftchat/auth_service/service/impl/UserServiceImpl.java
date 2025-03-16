package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.dto.UserCreateDto;
import com.swiftchat.auth_service.dto.UserDto;
import com.swiftchat.auth_service.dto.UserUpdateDto;
import com.swiftchat.auth_service.event.UserCreatedEvent;
import com.swiftchat.auth_service.exception.ResourceNotFoundException;
import com.swiftchat.auth_service.exception.UserAlreadyExistsException;
import com.swiftchat.auth_service.model.Role;
import com.swiftchat.auth_service.model.RoleName;
import com.swiftchat.auth_service.model.User;
import com.swiftchat.auth_service.model.UserRole;
import com.swiftchat.auth_service.repository.RoleRepository;
import com.swiftchat.auth_service.repository.UserRepository;
import com.swiftchat.auth_service.repository.UserRoleRepository;
import com.swiftchat.auth_service.service.EmailService;
import com.swiftchat.auth_service.service.UserService;
import com.swiftchat.auth_service.util.RandomUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;
    private final EmailService emailService;

    @Override
    @Transactional
    public User createUser(UserCreateDto userCreateDto) {
        if (userRepository.existsByEmail(userCreateDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use: " + userCreateDto.getEmail());
        }

        User user = User.builder()
                .email(userCreateDto.getEmail())
                .passwordHash(passwordEncoder.encode(userCreateDto.getPassword()))
                .activated(false)
                .activationKey(RandomUtil.generateActivationKey())
                .createdAt(LocalDateTime.now())
                .roles(new HashSet<>())
                .build();

        // Save user first without roles
        user = userRepository.save(user);

        // Add default user role in a separate transaction
        try {
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                    .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));
            // Only add if not already present
            if (!user.getRoles().contains(userRole)) {
                user.getRoles().add(userRole);
                // Re-save the user to update the join table managed by Hibernate
                user = userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("Error assigning default role to user: {}", e.getMessage());
            // Continue with user creation even if role assignment fails
        }

        // Send activation email
        try {
            emailService.sendActivationEmail(user.getEmail(), user.getActivationKey());
        } catch (Exception e) {
            log.warn("Failed to send activation email, continuing: {}", e.getMessage());
        }

        // Publish user created event with graceful degradation
        try {
            publishUserCreatedEvent(user);
        } catch (Exception e) {
            log.warn("Failed to publish user creation event, continuing: {}", e.getMessage());
        }

        log.info("Created new user: {}", user.getEmail());
        return user;
    }

    @Override
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUser(UUID id, UserUpdateDto userUpdateDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (userUpdateDto.getPassword() != null && !userUpdateDto.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(userUpdateDto.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getEmail());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void activateUser(String activationKey) {
        User user = userRepository.findByActivationKey(activationKey)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No user found with activation key: " + activationKey));

        user.setActivated(true);
        user.setActivationKey(null);
        userRepository.save(user);
        log.info("Activated user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + email));

        user.setResetKey(RandomUtil.generateResetKey());
        user = userRepository.save(user);

        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getResetKey());
            log.info("Password reset requested for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
            // Continue even if email sending fails, to avoid leaking user information
        }
    }

    @Override
    @Transactional
    public void completePasswordReset(String resetKey, String newPassword) {
        User user = userRepository.findByResetKey(resetKey)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with reset key: " + resetKey));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetKey(null);
        userRepository.save(user);
        log.info("Password reset completed for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void updateLastLogin(UUID id, LocalDateTime loginTime) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.updateLastLogin(id, loginTime);
    }

    @Override
    public boolean checkUserActivated(UUID id) {
        return userRepository.findById(id)
                .map(User::isActivated)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public UserDto mapToDto(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .activated(user.isActivated())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .roles(roles)
                .build();
    }

    @Override
    public List<UserDto> mapToDtoList(List<User> users) {
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addRoleToUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        // Check if the user already has this role
        if (userRoleRepository.existsByUserAndRole(user, role)) {
            log.debug("User {} already has role {}, skipping assignment", userId, roleName);
            return;
        }

        try {
            // Create and save the user-role association
            UserRole.UserRoleId userRoleId = new UserRole.UserRoleId(userId, role.getId());
            UserRole userRole = UserRole.builder()
                    .id(userRoleId)
                    .user(user)
                    .role(role)
                    .build();
            userRoleRepository.save(userRole);

            // Update the user's role collection
            user.getRoles().add(role);
        } catch (Exception e) {
            // Handle duplicate key issues
            log.warn("Attempted to add duplicate role {} to user {}, ignoring", roleName, userId);
            // Re-fetch from database to ensure consistency
            user.setRoles(new HashSet<>(userRepository.findById(userId)
                    .map(User::getRoles)
                    .orElse(new HashSet<>())));
        }
    }

    @Override
    @Transactional
    public void removeRoleFromUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        userRoleRepository.findByUserAndRole(user, role)
                .ifPresent(userRole -> {
                    userRoleRepository.delete(userRole);
                    user.getRoles().remove(role);
                });
    }

    @Override
    public boolean hasRole(UUID userId, String roleName) {
        return userRepository.hasRole(userId, roleName);
    }

    private void publishUserCreatedEvent(User user) {
        try {
            UserCreatedEvent event = UserCreatedEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .timestamp(System.currentTimeMillis())
                    .build();

            CompletableFuture<SendResult<String, UserCreatedEvent>> future = kafkaTemplate.send("user-created", event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published user created event for {}", user.getEmail());
                } else {
                    log.warn("Unable to publish user created event: {}", ex.getMessage());
                }
            });

            // Don't block the thread waiting for Kafka
        } catch (Exception e) {
            // Log error but don't prevent user creation
            log.error("Failed to publish user created event: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Kafka error details", e);
            }
        }
    }
}
