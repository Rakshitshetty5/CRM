package com.flowcrm.notification.service;

import com.flowcrm.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    NotificationResponse createNotification(UUID userId, UUID organizationId, String type, String title, String message, UUID referenceId);

    Page<NotificationResponse> getUserNotifications(Pageable pageable);

    NotificationResponse markAsRead(UUID notificationId);
}
