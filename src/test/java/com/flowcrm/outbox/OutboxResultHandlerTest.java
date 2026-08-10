package com.flowcrm.outbox;

import com.flowcrm.common.enums.OutboxStatus;
import com.flowcrm.outbox.entity.OutboxEvent;
import com.flowcrm.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxResultHandlerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxResultHandler outboxResultHandler;

    @BeforeEach
    void setUp() {
        outboxResultHandler = new OutboxResultHandler(outboxEventRepository);
        ReflectionTestUtils.setField(outboxResultHandler, "maxAttempts", 3);
    }

    @Test
    void testHandleEventPublished() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxResultHandler.handleEventPublished(event);

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    void testHandleEventFailedBelowMaxAttempts() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxResultHandler.handleEventFailed(event, "Kafka timeout");

        assertEquals(1, event.getRetryCount());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals("Kafka timeout", event.getErrorMessage());
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    void testHandleEventFailedReachingMaxAttempts() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .status(OutboxStatus.PENDING)
                .retryCount(2)
                .build();

        outboxResultHandler.handleEventFailed(event, "Broker unreachable");

        assertEquals(3, event.getRetryCount());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals("Broker unreachable", event.getErrorMessage());
        verify(outboxEventRepository, times(1)).save(event);
    }
}
