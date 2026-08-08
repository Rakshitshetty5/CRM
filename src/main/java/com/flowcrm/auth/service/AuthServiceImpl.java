package com.flowcrm.auth.service;

import com.flowcrm.auth.dto.AuthResponse;
import com.flowcrm.auth.dto.LoginRequest;
import com.flowcrm.auth.dto.RefreshTokenRequest;
import com.flowcrm.auth.dto.RegisterRequest;
import com.flowcrm.auth.dto.UserResponse;
import com.flowcrm.auth.entity.RefreshToken;
import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.RefreshTokenRepository;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.auth.security.JwtService;
import com.flowcrm.auth.security.UserPrincipal;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.exception.EmailAlreadyExistsException;
import com.flowcrm.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.SALES_REP)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String rawRefreshToken = jwtService.generateRawRefreshToken();
        String hashedRefreshToken = jwtService.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(hashedRefreshToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtService.getRefreshTokenExpirationMs())))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationInSeconds()
        );
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawRefreshToken = request.refreshToken();
        String hashedRefreshToken = jwtService.hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByToken(hashedRefreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        // Revoke the old refresh token (refresh token rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        if (!user.isActive()) {
            throw new InvalidTokenException("User account is inactive");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken = jwtService.generateAccessToken(userPrincipal);
        String newRawRefreshToken = jwtService.generateRawRefreshToken();
        String newHashedRefreshToken = jwtService.hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newHashedRefreshToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtService.getRefreshTokenExpirationMs())))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return new AuthResponse(
                newAccessToken,
                newRawRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationInSeconds()
        );
    }
}
