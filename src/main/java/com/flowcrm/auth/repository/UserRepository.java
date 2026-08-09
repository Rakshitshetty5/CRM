package com.flowcrm.auth.repository;

import com.flowcrm.auth.entity.User;
import com.flowcrm.common.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByActive(boolean active, Pageable pageable);
    Page<User> findByRoleAndActive(Role role, boolean active, Pageable pageable);

    Page<User> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<User> findByOrganizationIdAndRole(UUID organizationId, Role role, Pageable pageable);
    Page<User> findByOrganizationIdAndActive(UUID organizationId, boolean active, Pageable pageable);
    Page<User> findByOrganizationIdAndRoleAndActive(UUID organizationId, Role role, boolean active, Pageable pageable);
    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
