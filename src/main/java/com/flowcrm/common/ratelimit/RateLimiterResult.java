package com.flowcrm.common.ratelimit;

public record RateLimiterResult(
        boolean isAllowed,
        int remaining,
        int retryAfterSeconds
) {

    public static RateLimiterResult allowed(int remaining) {
        return new RateLimiterResult(true, remaining, 0);
    }

    public static RateLimiterResult denied(int retryAfterSeconds) {
        return new RateLimiterResult(false, 0, retryAfterSeconds);
    }
}
