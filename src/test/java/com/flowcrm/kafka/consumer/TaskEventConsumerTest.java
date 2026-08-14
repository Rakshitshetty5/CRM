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
class TaskEventConsumerTest {

    @Mock
    private EventProcessingService eventProcessingService;

    private TaskEventConsumer taskEventConsumer;

    @BeforeEach
    void setUp() {
        taskEventConsumer = new TaskEventConsumer(eventProcessingService, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }


    @Test
    void testConsumeTaskEvent() {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, "TaskCreated", "TASK", UUID.randomUUID(), "{}");

        taskEventConsumer.consumeTaskEvent(envelope);

        verify(eventProcessingService, times(1)).processEvent(envelope);
    }

    @Test
    void testHandleDlt() {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, "TaskCreated", "TASK", UUID.randomUUID(), "{}");

        taskEventConsumer.handleDlt(envelope);

        verifyNoInteractions(eventProcessingService);
    }
}
