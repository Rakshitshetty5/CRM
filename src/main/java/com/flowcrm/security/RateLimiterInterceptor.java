package com.flowcrm.security;

import com.flowcrm.common.config.RateLimitProperties;
import com.flowcrm.common.exception.RateLimitExceededException;
import com.flowcrm.common.ratelimit.RateLimiter;
import com.flowcrm.common.ratelimit.RateLimiterResult;
import com.flowcrm.common.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimitProperties rateLimitProperties;
    private final RateLimiter rateLimiter;
    private final UserContext userContext;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        UUID userId = userContext.getUserId();
        String rateLimitKey = userId != null ? "user:" + userId : "ip:" + getClientIp(request);
        int maxRequests = rateLimitProperties.getRequestsPerMinute();

        RateLimiterResult result = rateLimiter.check(rateLimitKey, maxRequests, 60L);

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for key={}, retryAfterSeconds={}", rateLimitKey, result.retryAfterSeconds());
            throw new RateLimitExceededException("Too many requests. Please try again later.", result.retryAfterSeconds());
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        }
        return xfHeader.split(",")[0].trim();
    }
}
