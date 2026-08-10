package com.flowcrm.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.common.enums.OutboxStatus;
import com.flowcrm.common.event.LeadCreatedEvent;
import com.flowcrm.outbox.entity.OutboxEvent;
import com.flowcrm.outbox.repository.OutboxEventRepository;
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
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventPublisher = new OutboxEventPublisher(outboxEventRepository);
    }

    @Test
    void testPublishLeadCreatedEvent() {
        UUID orgId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID assignedTo = UUID.randomUUID();

        LeadCreatedEvent event = new LeadCreatedEvent(orgId, leadId, assignedTo);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent result = outboxEventPublisher.publish(event);

        assertNotNull(result);
        assertEquals("LEAD", result.getAggregateType());
        assertEquals(leadId, result.getAggregateId());
        assertEquals("LeadCreated", result.getEventType());
        assertEquals(OutboxStatus.PENDING, result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNotNull(result.getPayload());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());
        assertEquals("LEAD", captor.getValue().getAggregateType());
    }
}
