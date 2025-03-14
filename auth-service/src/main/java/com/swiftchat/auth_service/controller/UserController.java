package com.swiftchat.auth_service.controller;

import com.swiftchat.auth_service.dto.UserDto;
import com.swiftchat.auth_service.dto.UserUpdateDto;
import com.swiftchat.auth_service.service.AuthService;
import com.swiftchat.auth_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieves the profile of the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        log.info("Fetching current user profile");
        UserDto userDto = userService.mapToDto(authService.getCurrentUser());
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user profile by their unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDto> getUserById(
            @PathVariable @Parameter(description = "User ID", required = true) UUID id) {

        log.info("Fetching user with ID: {}", id);
        return userService.getUserById(id)
                .map(userService::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates a user's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this user")
    })
    @PreAuthorize("@authService.getCurrentUserId() == #id or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable @Parameter(description = "User ID", required = true) UUID id,
            @Valid @RequestBody @Parameter(description = "Updated user data", required = true) UserUpdateDto updateDto) {

        log.info("Updating user with ID: {}", id);
        UserDto updatedUser = userService.mapToDto(userService.updateUser(id, updateDto));
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Permanently deletes a user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this user")
    })
    @PreAuthorize("@authService.getCurrentUserId() == #id or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Parameter(description = "User ID", required = true) UUID id) {

        log.info("Deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves all user profiles (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized to view all users")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("Fetching all users (admin only)");
        List<UserDto> users = userService.mapToDtoList(userService.getAllUsers());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @Operation(summary = "Add role to user", description = "Assigns a role to a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to assign roles")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> addRoleToUser(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @PathVariable @Parameter(description = "Role name", required = true) String roleName) {

        log.info("Adding role '{}' to user: {}", roleName, userId);
        userService.addRoleToUser(userId, roleName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @Operation(summary = "Remove role from user", description = "Removes a role from a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to remove roles")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @PathVariable @Parameter(description = "Role name", required = true) String roleName) {

        log.info("Removing role '{}' from user: {}", roleName, userId);
        userService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok().build();
    }
}
