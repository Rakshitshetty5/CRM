package com.flowcrm.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        UUID organizationId,
        String type,
        String title,
        String message,
        UUID referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {}
