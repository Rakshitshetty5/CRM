package com.flowcrm.notification.service;

import com.flowcrm.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    default NotificationResponse createNotification(UUID userId, UUID organizationId, String type, String title, String message, UUID referenceId) {
        return createNotification(userId, organizationId, type, title, message, referenceId, null);
    }

    NotificationResponse createNotification(UUID userId, UUID organizationId, String type, String title, String message, UUID referenceId, Map<String, Object> metadata);

    Page<NotificationResponse> getUserNotifications(Pageable pageable);

    NotificationResponse markAsRead(UUID notificationId);
}
