package com.flowcrm.kafka.consumer;

import com.flowcrm.kafka.dto.EventEnvelope;
import com.flowcrm.kafka.service.EventProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadEventConsumerTest {

    @Mock
    private EventProcessingService eventProcessingService;

    private LeadEventConsumer leadEventConsumer;

    @BeforeEach
    void setUp() {
        leadEventConsumer = new LeadEventConsumer(eventProcessingService);
    }

    @Test
    void testConsumeLeadEvent() {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, "LeadCreated", "LEAD", UUID.randomUUID(), "{}");

        leadEventConsumer.consumeLeadEvent(envelope);

        verify(eventProcessingService, times(1)).processEvent(envelope);
    }

    @Test
    void testHandleDlt() {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, "LeadCreated", "LEAD", UUID.randomUUID(), "{}");

        leadEventConsumer.handleDlt(envelope);

        verifyNoInteractions(eventProcessingService);
    }
}
