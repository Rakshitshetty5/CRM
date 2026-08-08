package com.flowcrm.auth.service;

import com.flowcrm.auth.dto.AuthResponse;
import com.flowcrm.auth.dto.LoginRequest;
import com.flowcrm.auth.dto.RefreshTokenRequest;
import com.flowcrm.auth.dto.RegisterRequest;
import com.flowcrm.auth.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
}
