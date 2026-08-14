package com.flowcrm.notification.service;

import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.notification.dto.NotificationResponse;
import com.flowcrm.notification.entity.Notification;
import com.flowcrm.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserContext userContext;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userContext, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }


    @Test
    void testCreateNotification() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .organizationId(orgId)
                .type("LEAD_ASSIGNED")
                .title("New Lead Assigned")
                .message("A new lead has been assigned to you.")
                .referenceId(refId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationResponse response = notificationService.createNotification(
                userId, orgId, "LEAD_ASSIGNED", "New Lead Assigned", "A new lead has been assigned to you.", refId
        );

        assertNotNull(response);
        assertEquals(saved.getId(), response.id());
        assertEquals(userId, response.userId());
        assertEquals(orgId, response.organizationId());
        assertEquals("LEAD_ASSIGNED", response.type());
        assertFalse(response.isRead());
        assertNull(response.metadata());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testCreateNotificationWithMetadata() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        java.util.Map<String, Object> metadata = java.util.Map.of("leadId", refId.toString(), "leadName", "Acme Corp");

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .organizationId(orgId)
                .type("LEAD_ASSIGNED")
                .title("New Lead Assigned")
                .message("A new lead has been assigned to you.")
                .referenceId(refId)
                .isRead(false)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationResponse response = notificationService.createNotification(
                userId, orgId, "LEAD_ASSIGNED", "New Lead Assigned", "A new lead has been assigned to you.", refId, metadata
        );

        assertNotNull(response);
        assertEquals(saved.getId(), response.id());
        assertNotNull(response.metadata());
        assertEquals("Acme Corp", response.metadata().get("leadName"));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGetUserNotifications() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(userContext.getUserId()).thenReturn(userId);
        when(userContext.getOrganizationId()).thenReturn(orgId);

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .organizationId(orgId)
                .type("LEAD_ASSIGNED")
                .title("New Lead Assigned")
                .message("Message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Notification> notificationPage = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdAndOrganizationIdOrderByCreatedAtDesc(userId, orgId, pageable))
                .thenReturn(notificationPage);

        Page<NotificationResponse> result = notificationService.getUserNotifications(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(notification.getId(), result.getContent().get(0).id());
    }

    @Test
    void testMarkAsReadSuccess() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        when(userContext.getUserId()).thenReturn(userId);
        when(userContext.getOrganizationId()).thenReturn(orgId);

        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .organizationId(orgId)
                .type("LEAD_ASSIGNED")
                .title("Title")
                .message("Message")
                .isRead(false)
                .build();

        when(notificationRepository.findByIdAndUserIdAndOrganizationId(notificationId, userId, orgId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(notificationId);

        assertNotNull(response);
        assertTrue(response.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void testMarkAsReadNotFound() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        when(userContext.getUserId()).thenReturn(userId);
        when(userContext.getOrganizationId()).thenReturn(orgId);
        when(notificationRepository.findByIdAndUserIdAndOrganizationId(notificationId, userId, orgId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(notificationId));
    }
}
