package com.flowcrm.security;

import com.flowcrm.common.config.RateLimitProperties;
import com.flowcrm.common.exception.RateLimitExceededException;
import com.flowcrm.common.ratelimit.RateLimiter;
import com.flowcrm.common.ratelimit.RateLimiterResult;
import com.flowcrm.common.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterInterceptorTest {

    @Mock
    private RateLimitProperties rateLimitProperties;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private UserContext userContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimiterInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimiterInterceptor(rateLimitProperties, rateLimiter, userContext, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }


    @Test
    void testDisabledRateLimitingAllowsRequest() {
        when(rateLimitProperties.isEnabled()).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void testUnauthenticatedUserUsesIpRateLimiting() {
        when(rateLimitProperties.isEnabled()).thenReturn(true);
        when(rateLimitProperties.getRequestsPerMinute()).thenReturn(60);
        when(userContext.getUserId()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        when(rateLimiter.check("ip:192.168.1.100", 60, 60L))
                .thenReturn(RateLimiterResult.allowed(59));

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }

    @Test
    void testRequestWithinLimitSucceeds() {
        UUID userId = UUID.randomUUID();
        when(rateLimitProperties.isEnabled()).thenReturn(true);
        when(rateLimitProperties.getRequestsPerMinute()).thenReturn(60);
        when(userContext.getUserId()).thenReturn(userId);

        when(rateLimiter.check("user:" + userId, 60, 60L))
                .thenReturn(RateLimiterResult.allowed(59));

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }

    @Test
    void testRequestExceedingLimitThrowsRateLimitExceededException() {
        UUID userId = UUID.randomUUID();
        when(rateLimitProperties.isEnabled()).thenReturn(true);
        when(rateLimitProperties.getRequestsPerMinute()).thenReturn(60);
        when(userContext.getUserId()).thenReturn(userId);

        when(rateLimiter.check("user:" + userId, 60, 60L))
                .thenReturn(RateLimiterResult.denied(15));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, new Object())
        );

        assertEquals("Too many requests. Please try again later.", exception.getMessage());
        assertEquals(15L, exception.getRetryAfterSeconds());
    }
}
