package com.flowcrm.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.kafka.dto.EventEnvelope;
import com.flowcrm.kafka.entity.ProcessedEvent;
import com.flowcrm.kafka.repository.ProcessedEventRepository;
import com.flowcrm.notification.service.NotificationMetadataEnricher;
import com.flowcrm.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMetadataEnricher notificationMetadataEnricher;

    private ObjectMapper objectMapper;
    private EventProcessingService eventProcessingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventProcessingService = new EventProcessingService(processedEventRepository, objectMapper, notificationService, notificationMetadataEnricher, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }


    @Test
    void testProcessNewEventSuccessfully() {
        UUID eventId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String payload = "{\"organizationId\":\"" + orgId + "\",\"leadId\":\"" + aggregateId + "\"}";

        EventEnvelope envelope = new EventEnvelope(eventId, "LeadCreated", "LEAD", aggregateId, payload);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        boolean processed = eventProcessingService.processEvent(envelope);

        assertTrue(processed);
        verify(processedEventRepository, times(1)).existsById(eventId);

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository, times(1)).saveAndFlush(captor.capture());
        assertEquals(eventId, captor.getValue().getEventId());
        assertNotNull(captor.getValue().getProcessedAt());
        verifyNoInteractions(notificationService);
    }

    @Test
    void testProcessLeadAssignedEventCreatesNotification() {
        UUID eventId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID assignedTo = UUID.randomUUID();
        String payload = String.format("{\"organizationId\":\"%s\",\"leadId\":\"%s\",\"assignedTo\":\"%s\"}", orgId, leadId, assignedTo);

        EventEnvelope envelope = new EventEnvelope(eventId, "LeadAssigned", "LEAD", leadId, payload);
        Map<String, Object> mockMetadata = Map.of("leadId", leadId.toString(), "stage", "NEW");

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(notificationMetadataEnricher.buildLeadAssignedMetadata(eq(leadId), any())).thenReturn(mockMetadata);

        boolean processed = eventProcessingService.processEvent(envelope);

        assertTrue(processed);
        verify(notificationService, times(1)).createNotification(
                eq(assignedTo),
                eq(orgId),
                eq("LEAD_ASSIGNED"),
                eq("New Lead Assigned"),
                eq("A new lead has been assigned to you."),
                eq(leadId),
                eq(mockMetadata)
        );
        verify(processedEventRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void testProcessLeadAssignedEventNotificationFailurePropagatesException() {
        UUID eventId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID assignedTo = UUID.randomUUID();
        String payload = String.format("{\"organizationId\":\"%s\",\"leadId\":\"%s\",\"assignedTo\":\"%s\"}", orgId, leadId, assignedTo);

        EventEnvelope envelope = new EventEnvelope(eventId, "LeadAssigned", "LEAD", leadId, payload);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> eventProcessingService.processEvent(envelope));

        verify(processedEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void testSkipDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, "LeadAssigned", "LEAD", aggregateId, "{}");

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        boolean processed = eventProcessingService.processEvent(envelope);

        assertFalse(processed);
        verify(processedEventRepository, times(1)).existsById(eventId);
        verifyNoInteractions(notificationService);
        verify(processedEventRepository, never()).save(any());
        verify(processedEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void testProcessTaskFollowUpDueEventCreatesNotification() {
        UUID eventId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID assignedTo = UUID.randomUUID();
        String payload = String.format("{\"organizationId\":\"%s\",\"taskId\":\"%s\",\"assignedTo\":\"%s\"}", orgId, taskId, assignedTo);

        EventEnvelope envelope = new EventEnvelope(eventId, "TaskFollowUpDue", "TASK", taskId, payload);
        Map<String, Object> mockMetadata = Map.of("taskId", taskId.toString(), "taskTitle", "Test Task");

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(notificationMetadataEnricher.buildTaskFollowUpDueMetadata(eq(taskId), any())).thenReturn(mockMetadata);

        boolean processed = eventProcessingService.processEvent(envelope);

        assertTrue(processed);
        verify(notificationService, times(1)).createNotification(
                eq(assignedTo),
                eq(orgId),
                eq("TASK_FOLLOW_UP_DUE"),
                eq("Task Follow-up Due"),
                eq("Your follow-up task is due."),
                eq(taskId),
                eq(mockMetadata)
        );
        verify(processedEventRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void testProcessTaskFollowUpDueEventNotificationFailurePropagatesException() {
        UUID eventId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID assignedTo = UUID.randomUUID();
        String payload = String.format("{\"organizationId\":\"%s\",\"taskId\":\"%s\",\"assignedTo\":\"%s\"}", orgId, taskId, assignedTo);

        EventEnvelope envelope = new EventEnvelope(eventId, "TaskFollowUpDue", "TASK", taskId, payload);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Notification service failure"));

        assertThrows(RuntimeException.class, () -> eventProcessingService.processEvent(envelope));

        verify(processedEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void testProcessInvalidEnvelope() {
        boolean processedNull = eventProcessingService.processEvent(null);
        assertFalse(processedNull);

        EventEnvelope emptyEnvelope = new EventEnvelope(null, "LeadCreated", "LEAD", UUID.randomUUID(), "{}");
        boolean processedEmpty = eventProcessingService.processEvent(emptyEnvelope);
        assertFalse(processedEmpty);

        verify(processedEventRepository, never()).existsById(any());
        verify(processedEventRepository, never()).save(any());
    }
}
