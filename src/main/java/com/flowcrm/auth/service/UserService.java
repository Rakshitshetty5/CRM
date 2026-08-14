package com.flowcrm.auth.service;

import com.flowcrm.auth.dto.CreateUserRequest;
import com.flowcrm.auth.dto.UserResponse;
import com.flowcrm.common.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    UserResponse getCurrentUser();
    UserResponse createUser(CreateUserRequest request);
    Page<UserResponse> getUsers(Role role, Boolean active, Pageable pageable);
    UserResponse getUserById(UUID userId);
    UserResponse updateUserStatus(UUID userId, boolean active);
}
