package com.swiftchat.auth_service.service;

import com.swiftchat.auth_service.dto.UserCreateDto;
import com.swiftchat.auth_service.dto.UserDto;
import com.swiftchat.auth_service.dto.UserUpdateDto;
import com.swiftchat.shared.security.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    User createUser(UserCreateDto userCreateDto);

    Optional<User> getUserById(UUID id);

    Optional<User> getUserByEmail(String email);

    List<User> getAllUsers();

    User updateUser(UUID id, UserUpdateDto userUpdateDto);

    void deleteUser(UUID id);

    boolean existsByEmail(String email);

    void activateUser(String activationKey);

    void requestPasswordReset(String email);

    void completePasswordReset(String resetKey, String newPassword);

    void updateLastLogin(UUID id, LocalDateTime loginTime);

    boolean checkUserActivated(UUID id);

    UserDto mapToDto(User user);

    List<UserDto> mapToDtoList(List<User> users);

    void addRoleToUser(UUID userId, String roleName);

    void removeRoleFromUser(UUID userId, String roleName);

    boolean hasRole(UUID userId, String roleName);
}
