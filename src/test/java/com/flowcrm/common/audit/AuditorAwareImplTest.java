package com.flowcrm.common.audit;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.security.UserPrincipal;
import com.flowcrm.common.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditorAwareImplTest {

    private AuditorAwareImpl auditorAware;

    @BeforeEach
    void setUp() {
        auditorAware = new AuditorAwareImpl();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentAuditor returns user UUID when authenticated")
    void getCurrentAuditor_Authenticated() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .role(Role.SALES_REP)
                .active(true)
                .build();
        UserPrincipal principal = new UserPrincipal(user);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> currentAuditor = auditorAware.getCurrentAuditor();
        assertTrue(currentAuditor.isPresent());
        assertEquals(userId, currentAuditor.get());
    }

    @Test
    @DisplayName("getCurrentAuditor returns empty when unauthenticated")
    void getCurrentAuditor_Unauthenticated() {
        Optional<UUID> currentAuditor = auditorAware.getCurrentAuditor();
        assertTrue(currentAuditor.isEmpty());
    }
}
