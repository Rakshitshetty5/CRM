package com.flowcrm.common.config;

import com.flowcrm.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Component("dashboardCacheKeyGenerator")
@RequiredArgsConstructor
public class DashboardCacheKeyGenerator implements KeyGenerator {

    private final UserContext userContext;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        UUID orgId = userContext.getOrganizationId();
        UUID userId = userContext.getUserId();
        return "org:" + orgId + ":user:" + userId;
    }
}
