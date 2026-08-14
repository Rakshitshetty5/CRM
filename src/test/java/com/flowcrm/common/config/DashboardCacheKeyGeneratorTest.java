package com.flowcrm.common.config;

import com.flowcrm.common.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCacheKeyGeneratorTest {

    @Mock
    private UserContext userContext;

    private DashboardCacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new DashboardCacheKeyGenerator(userContext);
    }

    @Test
    void testGenerateKeyIncludesOrgIdAndUserId() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(userContext.getOrganizationId()).thenReturn(orgId);
        when(userContext.getUserId()).thenReturn(userId);

        Object key = keyGenerator.generate(this, null);

        assertEquals("org:" + orgId + ":user:" + userId, key);
    }
}
