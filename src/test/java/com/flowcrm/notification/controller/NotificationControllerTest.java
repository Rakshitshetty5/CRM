package com.flowcrm.notification.controller;

import com.flowcrm.notification.dto.NotificationResponse;
import com.flowcrm.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController(notificationService);
    }

    @Test
    void testGetNotifications() {
        NotificationResponse responseDto = new NotificationResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "LEAD_ASSIGNED", "New Lead Assigned", "Message",
                UUID.randomUUID(), false, LocalDateTime.now()
        );

        Page<NotificationResponse> page = new PageImpl<>(List.of(responseDto));
        when(notificationService.getUserNotifications(any(PageRequest.class))).thenReturn(page);

        ResponseEntity<Page<NotificationResponse>> responseEntity = notificationController.getNotifications(0, 20);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().getTotalElements());
        verify(notificationService, times(1)).getUserNotifications(PageRequest.of(0, 20));
    }

    @Test
    void testMarkAsRead() {
        UUID notificationId = UUID.randomUUID();
        NotificationResponse responseDto = new NotificationResponse(
                notificationId, UUID.randomUUID(), UUID.randomUUID(),
                "LEAD_ASSIGNED", "New Lead Assigned", "Message",
                UUID.randomUUID(), true, LocalDateTime.now()
        );

        when(notificationService.markAsRead(notificationId)).thenReturn(responseDto);

        ResponseEntity<com.flowcrm.common.response.ApiResponse<NotificationResponse>> responseEntity = notificationController.markAsRead(notificationId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().success());
        assertTrue(responseEntity.getBody().data().isRead());
        verify(notificationService, times(1)).markAsRead(notificationId);
    }

}
