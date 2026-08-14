package com.flowcrm.auth.service;

import com.flowcrm.auth.dto.CreateUserRequest;
import com.flowcrm.auth.dto.UserResponse;
import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.exception.EmailAlreadyExistsException;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserContext userContext;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Cacheable(value = "userProfile", key = "@userContext.getUserId()")
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User currentUser = getAuthenticatedUser();
        return mapToUserResponse(currentUser);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can create users");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        Role userRole = request.role() != null ? request.role() : Role.SALES_REP;
        if (userRole == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot create ADMIN user");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(userRole)
                .active(true)
                .organization(currentUser.getOrganization())
                .build();

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Role role, Boolean active, Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Page<User> usersPage;

        if (role != null && active != null) {
            usersPage = userRepository.findByOrganizationIdAndRoleAndActive(organizationId, role, active, pageable);
        } else if (role != null) {
            usersPage = userRepository.findByOrganizationIdAndRole(organizationId, role, pageable);
        } else if (active != null) {
            usersPage = userRepository.findByOrganizationIdAndActive(organizationId, active, pageable);
        } else {
            usersPage = userRepository.findByOrganizationId(organizationId, pageable);
        }

        return usersPage.map(this::mapToUserResponse);
    }

    @Override
    @Cacheable(value = "userProfile", key = "#userId")
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User currentUser = getAuthenticatedUser();
        UUID organizationId = currentUser.getOrganization().getId();

        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToUserResponse(user);
    }

    @Override
    @CacheEvict(value = "userProfile", key = "#userId")
    @Transactional
    public UserResponse updateUserStatus(UUID userId, boolean active) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can update user status");
        }

        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot change status of yourself");
        }

        UUID organizationId = currentUser.getOrganization().getId();
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.SALES_REP) {
            throw new IllegalArgumentException("Can only update status of SALES_REP users");
        }

        user.setActive(active);
        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }


    private User getAuthenticatedUser() {
        UUID userId = userContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
