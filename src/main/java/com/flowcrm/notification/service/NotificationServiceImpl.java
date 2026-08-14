package com.flowcrm.notification.service;

import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.notification.dto.NotificationResponse;
import com.flowcrm.notification.entity.Notification;
import com.flowcrm.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserContext userContext;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional
    public NotificationResponse createNotification(UUID userId, UUID organizationId, String type, String title, String message, UUID referenceId) {
        return createNotification(userId, organizationId, type, title, message, referenceId, null);
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(UUID userId, UUID organizationId, String type, String title, String message, UUID referenceId, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .organizationId(organizationId)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .metadata(metadata)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: id={}, userId={}, organizationId={}, type={}, referenceId={}",
                saved.getId(), userId, organizationId, type, referenceId);
        meterRegistry.counter("notifications.created", "type", type != null ? type : "unknown").increment();

        return mapToResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Pageable pageable) {
        UUID userId = userContext.getUserId();
        UUID organizationId = userContext.getOrganizationId();

        if (userId == null || organizationId == null) {
            throw new IllegalStateException("No authenticated user or organization found in security context");
        }

        Page<Notification> page = notificationRepository.findByUserIdAndOrganizationIdOrderByCreatedAtDesc(userId, organizationId, pageable);
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        UUID userId = userContext.getUserId();
        UUID organizationId = userContext.getOrganizationId();

        if (userId == null || organizationId == null) {
            throw new IllegalStateException("No authenticated user or organization found in security context");
        }

        Notification notification = notificationRepository.findByIdAndUserIdAndOrganizationId(notificationId, userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        log.info("Marked notification as read: id={}, userId={}", updated.getId(), userId);

        return mapToResponse(updated);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getOrganizationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getMetadata()
        );
    }
}
