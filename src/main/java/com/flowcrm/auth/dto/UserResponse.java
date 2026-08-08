package com.flowcrm.auth.dto;

import com.flowcrm.common.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role
) {
}