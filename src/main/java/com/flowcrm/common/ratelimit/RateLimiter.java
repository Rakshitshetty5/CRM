package com.flowcrm.common.ratelimit;

public interface RateLimiter {

    RateLimiterResult check(String key, int maxRequestAllowed, long windowSeconds);
}
