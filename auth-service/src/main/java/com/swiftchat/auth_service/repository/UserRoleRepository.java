package com.swiftchat.auth_service.repository;

import com.swiftchat.auth_service.model.Role;
import com.swiftchat.auth_service.model.User;
import com.swiftchat.auth_service.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    List<UserRole> findAllByUser(User user);

    List<UserRole> findAllByRole(Role role);

    Optional<UserRole> findByUserAndRole(User user, Role role);

    void deleteByUserAndRole(User user, Role role);

    boolean existsByUserAndRole(User user, Role role);
}
