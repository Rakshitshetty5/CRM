package com.flowcrm.outbox;

import com.flowcrm.common.enums.OutboxStatus;
import com.flowcrm.outbox.entity.OutboxEvent;
import com.flowcrm.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxResultHandler outboxResultHandler;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OutboxPoller outboxPoller;

    @BeforeEach
    void setUp() {
        outboxPoller = new OutboxPoller(outboxEventRepository, outboxResultHandler, kafkaTemplate);
        ReflectionTestUtils.setField(outboxPoller, "leadTopic", "leads.events");
        ReflectionTestUtils.setField(outboxPoller, "taskTopic", "tasks.events");
    }

    @Test
    void testPollAndPublishSuccess() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("LEAD")
                .aggregateId(aggregateId)
                .eventType("LeadCreated")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("leads.events"), eq(aggregateId.toString()), any())).thenReturn(future);

        outboxPoller.pollAndPublish();

        verify(outboxResultHandler, times(1)).handleEventPublished(event);
        verify(outboxResultHandler, never()).handleEventFailed(any(), any());
    }

    @Test
    void testPollAndPublishFailure() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("TASK")
                .aggregateId(aggregateId)
                .eventType("TaskCreated")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(eq("tasks.events"), eq(aggregateId.toString()), any())).thenReturn(future);

        outboxPoller.pollAndPublish();

        verify(outboxResultHandler, times(1)).handleEventFailed(eq(event), any());
        verify(outboxResultHandler, never()).handleEventPublished(any());
    }
}
