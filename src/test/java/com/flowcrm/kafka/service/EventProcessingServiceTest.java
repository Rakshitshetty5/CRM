package com.flowcrm.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.kafka.dto.EventEnvelope;
import com.flowcrm.kafka.entity.ProcessedEvent;
import com.flowcrm.kafka.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ObjectMapper objectMapper;
    private EventProcessingService eventProcessingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventProcessingService = new EventProcessingService(processedEventRepository, objectMapper);
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
    }

    @Test
    void testSkipDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, "LeadCreated", "LEAD", aggregateId, "{}");

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        boolean processed = eventProcessingService.processEvent(envelope);

        assertFalse(processed);
        verify(processedEventRepository, times(1)).existsById(eventId);
        verify(processedEventRepository, never()).save(any());
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
