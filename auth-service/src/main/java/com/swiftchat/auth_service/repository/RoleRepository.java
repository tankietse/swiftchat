package com.swiftchat.auth_service.repository;

import com.swiftchat.shared.security.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name = :name")
    Optional<Role> findByRoleName(@Param("name") String name);

    boolean existsByName(String name);
}
