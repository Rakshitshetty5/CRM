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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encoded_password")
                .role(Role.SALES_REP)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Register - Success")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john.doe@example.com", "Password123!");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(testUser.getId(), response.id());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("john.doe@example.com", response.email());
        assertEquals(Role.SALES_REP, response.role());

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Register - Throws EmailAlreadyExistsException when email exists")
    void register_EmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john.doe@example.com", "Password123!");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "Password123!");
        UserPrincipal userPrincipal = new UserPrincipal(testUser);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token");
        when(jwtService.generateRawRefreshToken()).thenReturn("raw_refresh_token");
        when(jwtService.hashToken("raw_refresh_token")).thenReturn("hashed_refresh_token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtService.getAccessTokenExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals("raw_refresh_token", response.refreshToken());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token Rotation - Success")
    void refresh_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("raw_old_refresh_token");

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("hashed_old_refresh_token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(jwtService.hashToken("raw_old_refresh_token")).thenReturn("hashed_old_refresh_token");
        when(refreshTokenRepository.findByToken("hashed_old_refresh_token")).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("new_access_token");
        when(jwtService.generateRawRefreshToken()).thenReturn("raw_new_refresh_token");
        when(jwtService.hashToken("raw_new_refresh_token")).thenReturn("hashed_new_refresh_token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtService.getAccessTokenExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.accessToken());
        assertEquals("raw_new_refresh_token", response.refreshToken());

        assertTrue(storedToken.isRevoked(), "Old refresh token must be revoked");

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(refreshTokenCaptor.capture());
        RefreshToken newSavedToken = refreshTokenCaptor.getAllValues().get(1);

        assertEquals("hashed_new_refresh_token", newSavedToken.getToken());
        assertFalse(newSavedToken.isRevoked());
    }

    @Test
    @DisplayName("Refresh Token - Throws InvalidTokenException when token is revoked or expired")
    void refresh_RevokedOrExpiredToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("raw_expired_token");

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("hashed_expired_token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusDays(1)) // expired
                .revoked(false)
                .build();

        when(jwtService.hashToken("raw_expired_token")).thenReturn("hashed_expired_token");
        when(refreshTokenRepository.findByToken("hashed_expired_token")).thenReturn(Optional.of(storedToken));

        assertThrows(InvalidTokenException.class, () -> authService.refresh(request));
        assertTrue(storedToken.isRevoked(), "Expired token should be marked revoked");
    }
}
