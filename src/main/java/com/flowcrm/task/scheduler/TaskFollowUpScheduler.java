package com.flowcrm.task.scheduler;

import com.flowcrm.common.event.TaskFollowUpDue;
import com.flowcrm.common.lock.RedisDistributedLockService;
import com.flowcrm.outbox.OutboxEventPublisher;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskFollowUpScheduler {

    private static final String LOCK_KEY = "lock:follow-up-reminder";
    private static final Duration LOCK_TTL = Duration.ofSeconds(120);

    private final TaskRepository taskRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final MeterRegistry meterRegistry;
    private final RedisDistributedLockService lockService;

    @Scheduled(fixedDelayString = "${app.follow-up.polling.fixed-delay:60000}")
    public void checkForDueTasks() {
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = lockService.tryLock(LOCK_KEY, lockValue, LOCK_TTL);
        if (!acquired) {
            log.debug("Follow-up reminder lock '{}' is held by another instance. Skipping execution.", LOCK_KEY);
            return;
        }

        try {
            log.debug("Polling for tasks due for follow-up reminders...");
            LocalDateTime now = LocalDateTime.now();
            List<Task> dueTasks = taskRepository.findTasksDueForReminder(now, PageRequest.of(0, 100));

            for (Task task : dueTasks) {
                try {
                    processTaskReminder(task);
                } catch (Exception e) {
                    log.error("Failed to process follow-up reminder for taskId={}", task.getId(), e);
                }
            }
        } finally {
            lockService.unlock(LOCK_KEY, lockValue);
        }
    }

    @Transactional
    public void processTaskReminder(Task task) {
        int updatedCount = taskRepository.markReminderSent(task.getId());
        if (updatedCount == 1) {
            UUID orgId = task.getOrganization() != null ? task.getOrganization().getId() : null;
            UUID assignedToId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;

            if (assignedToId != null) {
                TaskFollowUpDue event = new TaskFollowUpDue(orgId, task.getId(), assignedToId);
                outboxEventPublisher.publish(event);
                log.info("Published TaskFollowUpDue event for taskId={}, organizationId={}, assignedTo={}", task.getId(), orgId, assignedToId);
                meterRegistry.counter("reminders.processed").increment();
            }
        } else {
            log.debug("Task reminder already processed by another instance for taskId={}", task.getId());
        }
    }
}


