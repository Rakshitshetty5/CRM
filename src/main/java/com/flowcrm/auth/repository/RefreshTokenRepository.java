package com.flowcrm.auth.repository;

import com.flowcrm.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flowcrm.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    void deleteByUser(User user);
}
