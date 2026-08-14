package com.flowcrm.common.observability;

import com.flowcrm.common.config.RateLimitProperties;
import com.flowcrm.common.exception.RateLimitExceededException;
import com.flowcrm.common.lock.RedisDistributedLockService;
import com.flowcrm.common.ratelimit.RateLimiter;

import com.flowcrm.common.ratelimit.RateLimiterResult;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.kafka.consumer.LeadEventConsumer;
import com.flowcrm.kafka.consumer.TaskEventConsumer;
import com.flowcrm.kafka.dto.EventEnvelope;
import com.flowcrm.kafka.service.EventProcessingService;
import com.flowcrm.notification.dto.NotificationResponse;
import com.flowcrm.notification.entity.Notification;
import com.flowcrm.notification.repository.NotificationRepository;
import com.flowcrm.notification.service.NotificationServiceImpl;
import com.flowcrm.security.RateLimiterInterceptor;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
import com.flowcrm.task.scheduler.TaskFollowUpScheduler;
import com.flowcrm.outbox.OutboxEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class ObservabilityTest {

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    @DisplayName("RateLimiterInterceptor should increment rate_limit.rejections counter on rate limit violation")
    void rateLimiterInterceptor_IncrementsCounterOnRejection() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setRequestsPerMinute(10);

        RateLimiter rateLimiter = mock(RateLimiter.class);
        UserContext userContext = mock(UserContext.class);
        UUID userId = UUID.randomUUID();
        when(userContext.getUserId()).thenReturn(userId);
        when(rateLimiter.check(any(), anyInt(), anyLong()))
                .thenReturn(RateLimiterResult.denied(30));



        RateLimiterInterceptor interceptor = new RateLimiterInterceptor(properties, rateLimiter, userContext, meterRegistry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/leads");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(RateLimitExceededException.class, () -> interceptor.preHandle(request, response, new Object()));

        double count = meterRegistry.counter("rate_limit.rejections").count();
        assertEquals(1.0, count);
    }

    @Test
    @DisplayName("LeadEventConsumer & TaskEventConsumer DltHandler should increment kafka.events.dlt counter")
    void kafkaConsumer_IncrementsDltCounter() {
        EventProcessingService service = mock(EventProcessingService.class);
        LeadEventConsumer leadConsumer = new LeadEventConsumer(service, meterRegistry);
        TaskEventConsumer taskConsumer = new TaskEventConsumer(service, meterRegistry);

        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "LeadCreated", "LEAD", UUID.randomUUID(), "{}");

        leadConsumer.handleDlt(envelope);
        taskConsumer.handleDlt(envelope);

        double leadDltCount = meterRegistry.counter("kafka.events.dlt", "topic", "leads.events.DLT").count();
        double taskDltCount = meterRegistry.counter("kafka.events.dlt", "topic", "tasks.events.DLT").count();

        assertEquals(1.0, leadDltCount);
        assertEquals(1.0, taskDltCount);
    }

    @Test
    @DisplayName("NotificationServiceImpl should increment notifications.created counter")
    void notificationService_IncrementsCreatedCounter() {
        NotificationRepository repo = mock(NotificationRepository.class);
        UserContext userContext = mock(UserContext.class);
        NotificationServiceImpl service = new NotificationServiceImpl(repo, userContext, meterRegistry);

        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();

        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .organizationId(orgId)
                .type("LEAD_ASSIGNED")
                .referenceId(refId)
                .build();

        when(repo.save(any(Notification.class))).thenReturn(savedNotification);

        NotificationResponse response = service.createNotification(userId, orgId, "LEAD_ASSIGNED", "Title", "Message", refId);

        double count = meterRegistry.counter("notifications.created", "type", "LEAD_ASSIGNED").count();
        assertEquals(1.0, count);
    }

    @Test
    @DisplayName("TaskFollowUpScheduler should increment reminders.processed counter")
    void taskFollowUpScheduler_IncrementsProcessedCounter() {
        TaskRepository repository = mock(TaskRepository.class);
        OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);
        RedisDistributedLockService lockService = mock(RedisDistributedLockService.class);
        TaskFollowUpScheduler scheduler = new TaskFollowUpScheduler(repository, publisher, meterRegistry, lockService);


        Task task = mock(Task.class);
        UUID taskId = UUID.randomUUID();
        com.flowcrm.auth.entity.User assignedUser = mock(com.flowcrm.auth.entity.User.class);
        when(assignedUser.getId()).thenReturn(UUID.randomUUID());
        when(task.getId()).thenReturn(taskId);
        when(task.getAssignedTo()).thenReturn(assignedUser);
        when(repository.markReminderSent(taskId)).thenReturn(1);

        scheduler.processTaskReminder(task);


        double count = meterRegistry.counter("reminders.processed").count();
        assertEquals(1.0, count);
    }
}
