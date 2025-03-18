package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.dto.UserCreateDto;
import com.swiftchat.auth_service.dto.UserDto;
import com.swiftchat.auth_service.dto.UserUpdateDto;
import com.swiftchat.auth_service.event.UserCreatedEvent;
import com.swiftchat.auth_service.exception.ResourceNotFoundException;
import com.swiftchat.auth_service.exception.UserAlreadyExistsException;
import com.swiftchat.shared.security.model.Role;
import com.swiftchat.auth_service.model.RoleName;
import com.swiftchat.shared.security.model.User;
import com.swiftchat.auth_service.model.UserRole;
import com.swiftchat.auth_service.repository.RoleRepository;
import com.swiftchat.auth_service.repository.UserRepository;
import com.swiftchat.auth_service.repository.UserRoleRepository;
import com.swiftchat.auth_service.service.EmailService;
import com.swiftchat.auth_service.util.RandomUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Implementation Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role userRole;
    private UUID userId;
    private String activationKey;
    private String resetKey;
    private Set<Role> roles;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activationKey = "activation-key-123";
        resetKey = "reset-key-123";

        userRole = Role.builder()
                .id(UUID.randomUUID())
                .name(RoleName.ROLE_USER.name())
                .build();

        roles = new HashSet<>();
        roles.add(userRole);

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .activated(false)
                .activationKey(activationKey)
                .resetKey(resetKey)
                .createdAt(LocalDateTime.now())
                .roles(new HashSet<>())
                .build();
    }

    @Nested
    @DisplayName("User Creation Tests")
    class UserCreationTests {

        @Test
        @DisplayName("Should create user successfully when email doesn't exist")
        void createUser_Success_ShouldReturnCreatedUser() {
            // Arrange
            UserCreateDto createDto = new UserCreateDto("test@example.com", "password");
            String encodedPassword = "encodedPassword";

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(roleRepository.findByName(RoleName.ROLE_USER.name())).thenReturn(Optional.of(userRole));

            CompletableFuture<SendResult<String, UserCreatedEvent>> future = CompletableFuture
                    .completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(anyString(), any(UserCreatedEvent.class))).thenReturn(future);
            doNothing().when(emailService).sendActivationEmail(anyString(), anyString());

            try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
                randomUtilMock.when(RandomUtil::generateActivationKey).thenReturn(activationKey);

                // Act
                User createdUser = userService.createUser(createDto);

                // Assert
                assertNotNull(createdUser);
                assertEquals(testUser.getId(), createdUser.getId());
                assertEquals(testUser.getEmail(), createdUser.getEmail());

                // Capture and verify the saved user
                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
                verify(userRepository, times(2)).save(userCaptor.capture());
                User capturedUser = userCaptor.getAllValues().get(0);
                assertEquals(createDto.getEmail(), capturedUser.getEmail());
                assertEquals(encodedPassword, capturedUser.getPasswordHash());
                assertEquals(activationKey, capturedUser.getActivationKey());
                assertFalse(capturedUser.isActivated());

                verify(emailService).sendActivationEmail(eq(createDto.getEmail()), eq(activationKey));
                verify(kafkaTemplate).send(eq("user-created"), any(UserCreatedEvent.class));
            }
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email already exists")
        void createUser_EmailAlreadyExists_ShouldThrowUserAlreadyExistsException() {
            // Arrange
            UserCreateDto createDto = new UserCreateDto("existing@example.com", "password");
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // Act & Assert
            assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(createDto));

            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendActivationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Should still create user when role assignment fails")
        void createUser_RoleAssignmentFailure_ShouldStillCreateUser() {
            // Arrange
            UserCreateDto createDto = new UserCreateDto("test@example.com", "password");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(roleRepository.findByName(anyString())).thenReturn(Optional.empty()); // Role not found

            CompletableFuture<SendResult<String, UserCreatedEvent>> future = CompletableFuture
                    .completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(anyString(), any(UserCreatedEvent.class))).thenReturn(future);
            doNothing().when(emailService).sendActivationEmail(anyString(), anyString());

            try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
                randomUtilMock.when(RandomUtil::generateActivationKey).thenReturn(activationKey);

                // Act
                User createdUser = userService.createUser(createDto);

                // Assert
                assertNotNull(createdUser);
                verify(userRepository).save(any(User.class));
                // Even though role assignment failed, user should still be created
                verify(kafkaTemplate).send(eq("user-created"), any(UserCreatedEvent.class));
                verify(emailService).sendActivationEmail(anyString(), anyString());
            }
        }
    }

    @Nested
    @DisplayName("User Retrieval Tests")
    class UserRetrievalTests {

        @Test
        @DisplayName("Should return user when ID exists")
        void getUserById_UserExists_ShouldReturnUser() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // Act
            Optional<User> result = userService.getUserById(userId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(testUser.getId(), result.get().getId());
        }

        @Test
        @DisplayName("Should return empty Optional when ID doesn't exist")
        void getUserById_UserDoesNotExist_ShouldReturnEmptyOptional() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userService.getUserById(nonExistentId);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return user when email exists")
        void getUserByEmail_UserExists_ShouldReturnUser() {
            // Arrange
            String email = "test@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // Act
            Optional<User> result = userService.getUserByEmail(email);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(email, result.get().getEmail());
        }

        @Test
        @DisplayName("Should return all users")
        void getAllUsers_ShouldReturnAllUsers() {
            // Arrange
            List<User> users = Arrays.asList(testUser,
                    User.builder().id(UUID.randomUUID()).email("another@example.com").build());
            when(userRepository.findAll()).thenReturn(users);

            // Act
            List<User> result = userService.getAllUsers();

            // Assert
            assertEquals(2, result.size());
            assertEquals(users, result);
        }
    }

    @Nested
    @DisplayName("User Update Tests")
    class UserUpdateTests {

        @Test
        @DisplayName("Should update and return user when ID exists")
        void updateUser_UserExists_ShouldUpdateAndReturnUser() {
            // Arrange
            UserUpdateDto updateDto = new UserUpdateDto();
            updateDto.setPassword("newPassword");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            User updatedUser = userService.updateUser(userId, updateDto);

            // Assert
            assertNotNull(updatedUser);
            assertEquals("newEncodedPassword", updatedUser.getPasswordHash());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
        void updateUser_UserDoesNotExist_ShouldThrowResourceNotFoundException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            UserUpdateDto updateDto = new UserUpdateDto();
            updateDto.setPassword("newPassword");

            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(nonExistentId, updateDto));
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("User Deletion Tests")
    class UserDeletionTests {

        @Test
        @DisplayName("Should delete user when ID exists")
        void deleteUser_UserExists_ShouldDeleteUser() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).delete(any(User.class));

            // Act
            userService.deleteUser(userId);

            // Assert
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
        void deleteUser_UserDoesNotExist_ShouldThrowResourceNotFoundException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(nonExistentId));
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("User Validation Tests")
    class UserValidationTests {

        @ParameterizedTest
        @ValueSource(strings = { "existing@example.com", "another@example.com" })
        @DisplayName("Should check if email exists")
        void existsByEmail_EmailExists_ShouldReturnTrue(String email) {
            // Arrange
            when(userRepository.existsByEmail(email)).thenReturn(true);

            // Act
            boolean result = userService.existsByEmail(email);

            // Assert
            assertTrue(result);
            verify(userRepository).existsByEmail(email);
        }
    }

    @Nested
    @DisplayName("User Activation Tests")
    class UserActivationTests {

        @Test
        @DisplayName("Should activate user with valid activation key")
        void activateUser_ValidActivationKey_ShouldActivateUser() {
            // Arrange
            when(userRepository.findByActivationKey(activationKey)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            userService.activateUser(activationKey);

            // Assert
            assertTrue(testUser.isActivated());
            assertNull(testUser.getActivationKey());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException with invalid activation key")
        void activateUser_InvalidActivationKey_ShouldThrowResourceNotFoundException() {
            // Arrange
            String invalidKey = "invalid-key";
            when(userRepository.findByActivationKey(invalidKey)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> userService.activateUser(invalidKey));
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {

        @Test
        @DisplayName("Should set reset key and send email when requesting password reset")
        void requestPasswordReset_ValidEmail_ShouldSetResetKeyAndSendEmail() {
            // Arrange
            String email = "test@example.com";
            String newResetKey = "new-reset-key";

            try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
                randomUtilMock.when(RandomUtil::generateResetKey).thenReturn(newResetKey);

                when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

                // Act
                userService.requestPasswordReset(email);

                // Assert
                assertEquals(newResetKey, testUser.getResetKey());
                verify(emailService).sendPasswordResetEmail(email, newResetKey);
                verify(userRepository).save(testUser);
            }
        }

        @Test
        @DisplayName("Should update password with valid reset key")
        void completePasswordReset_ValidResetKey_ShouldUpdatePassword() {
            // Arrange
            String resetKey = "reset-key";
            String newPassword = "newPassword";
            String encodedPassword = "encodedNewPassword";

            when(userRepository.findByResetKey(resetKey)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            userService.completePasswordReset(resetKey, newPassword);

            // Assert
            assertEquals(encodedPassword, testUser.getPasswordHash());
            assertNull(testUser.getResetKey());
            verify(userRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("DTO Mapping Tests")
    class DtoMappingTests {

        @Test
        @DisplayName("Should map User to UserDto")
        void mapToDto_ShouldMapUserToUserDto() {
            // Arrange
            testUser.setRoles(roles);

            // Act
            UserDto userDto = userService.mapToDto(testUser);

            // Assert
            assertNotNull(userDto);
            assertEquals(testUser.getId(), userDto.getId());
            assertEquals(testUser.getEmail(), userDto.getEmail());
            assertEquals(testUser.isActivated(), userDto.isActivated());
            assertEquals(1, userDto.getRoles().size());
            assertTrue(userDto.getRoles().contains(RoleName.ROLE_USER.name()));
        }

        @Test
        @DisplayName("Should return null when mapping null User")
        void mapToDto_NullUser_ShouldReturnNull() {
            // Act
            UserDto userDto = userService.mapToDto(null);

            // Assert
            assertNull(userDto);
        }

        @Test
        @DisplayName("Should map User list to UserDto list")
        void mapToDtoList_ShouldMapUserListToUserDtoList() {
            // Arrange
            testUser.setRoles(roles);
            List<User> users = Arrays.asList(testUser);

            // Act
            List<UserDto> userDtos = userService.mapToDtoList(users);

            // Assert
            assertEquals(1, userDtos.size());
            assertEquals(testUser.getId(), userDtos.get(0).getId());
            assertEquals(testUser.getEmail(), userDtos.get(0).getEmail());
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should return empty list when mapping null list")
        void mapToDtoList_NullList_ShouldReturnEmptyList(List<User> nullList) {
            // Act
            List<UserDto> userDtos = userService.mapToDtoList(nullList);

            // Assert
            assertTrue(userDtos.isEmpty());
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagementTests {

        @Test
        @DisplayName("Should add new role to user")
        void addRoleToUser_NewRole_ShouldAddRoleToUser() {
            // Arrange
            Role adminRole = Role.builder()
                    .id(UUID.randomUUID())
                    .name(RoleName.ROLE_ADMIN.name())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(RoleName.ROLE_ADMIN.name())).thenReturn(Optional.of(adminRole));
            when(userRoleRepository.existsByUserAndRole(testUser, adminRole)).thenReturn(false);

            UserRole userRole = UserRole.builder()
                    .id(new UserRole.UserRoleId(userId, adminRole.getId()))
                    .user(testUser)
                    .role(adminRole)
                    .build();

            when(userRoleRepository.save(any(UserRole.class))).thenReturn(userRole);

            // Act
            userService.addRoleToUser(userId, RoleName.ROLE_ADMIN.name());

            // Assert
            verify(userRoleRepository).save(any(UserRole.class));
            assertTrue(testUser.getRoles().contains(adminRole));
        }

        @Test
        @DisplayName("Should not add duplicate role to user")
        void addRoleToUser_ExistingRole_ShouldNotAddDuplicate() {
            // Arrange
            Role adminRole = Role.builder()
                    .id(UUID.randomUUID())
                    .name(RoleName.ROLE_ADMIN.name())
                    .build();

            testUser.getRoles().add(userRole);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(RoleName.ROLE_USER.name())).thenReturn(Optional.of(userRole));
            when(userRoleRepository.existsByUserAndRole(testUser, userRole)).thenReturn(true);

            // Act
            userService.addRoleToUser(userId, RoleName.ROLE_USER.name());

            // Assert
            verify(userRoleRepository, never()).save(any(UserRole.class));
        }

        @Test
        @DisplayName("Should remove role from user")
        void removeRoleFromUser_ExistingRole_ShouldRemoveRole() {
            // Arrange
            Role adminRole = Role.builder()
                    .id(UUID.randomUUID())
                    .name(RoleName.ROLE_ADMIN.name())
                    .build();
            testUser.getRoles().add(adminRole);

            UserRole userRole = UserRole.builder()
                    .id(new UserRole.UserRoleId(userId, adminRole.getId()))
                    .user(testUser)
                    .role(adminRole)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(RoleName.ROLE_ADMIN.name())).thenReturn(Optional.of(adminRole));
            when(userRoleRepository.findByUserAndRole(testUser, adminRole)).thenReturn(Optional.of(userRole));
            doNothing().when(userRoleRepository).delete(any(UserRole.class));

            // Act
            userService.removeRoleFromUser(userId, RoleName.ROLE_ADMIN.name());

            // Assert
            verify(userRoleRepository).delete(userRole);
            assertFalse(testUser.getRoles().contains(adminRole));
        }

        @Test
        @DisplayName("Should do nothing when removing non-assigned role")
        void removeRoleFromUser_RoleNotAssignedToUser_ShouldDoNothing() {
            // Arrange
            Role adminRole = Role.builder()
                    .id(UUID.randomUUID())
                    .name(RoleName.ROLE_ADMIN.name())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(RoleName.ROLE_ADMIN.name())).thenReturn(Optional.of(adminRole));
            when(userRoleRepository.findByUserAndRole(testUser, adminRole)).thenReturn(Optional.empty());

            // Act
            userService.removeRoleFromUser(userId, RoleName.ROLE_ADMIN.name());

            // Assert
            verify(userRoleRepository, never()).delete(any(UserRole.class));
        }

        @ParameterizedTest
        @CsvSource({ "ROLE_USER,true", "ROLE_ADMIN,false" })
        @DisplayName("Should check if user has specific role")
        void hasRole_ShouldReturnCorrectResult(String roleName, boolean hasRole) {
            // Arrange
            when(userRepository.hasRole(userId, roleName)).thenReturn(hasRole);

            // Act
            boolean result = userService.hasRole(userId, roleName);

            // Assert
            assertEquals(hasRole, result);
            verify(userRepository).hasRole(userId, roleName);
        }
    }

    @Test
    @DisplayName("Should complete within timeout period")
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    void testWithTimeout() {
        // Simple performance test to ensure method completes within timeout
        userService.existsByEmail("test@example.com");
    }
}
