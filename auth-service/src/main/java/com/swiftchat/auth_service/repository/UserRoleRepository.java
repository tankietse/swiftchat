package com.swiftchat.auth_service.repository;

import com.swiftchat.shared.security.model.Role;
import com.swiftchat.shared.security.model.User;
import com.swiftchat.auth_service.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    List<UserRole> findAllByUser(User user);

    List<UserRole> findAllByRole(Role role);

    Optional<UserRole> findByUserAndRole(User user, Role role);

    void deleteByUserAndRole(User user, Role role);

    boolean existsByUserAndRole(User user, Role role);

    boolean existsById(UserRole.UserRoleId id);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.name = :roleName")
    Optional<UserRole> findByUserIdAndRoleName(@Param("userId") UUID userId, @Param("roleName") String roleName);
}
