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
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final TaskRepository taskRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final MeterRegistry meterRegistry;
    private final RedisDistributedLockService lockService;

    @Scheduled(fixedDelayString = "${app.follow-up.polling.fixed-delay:30000}")
    public void checkForDueTasks() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        log.info("TaskFollowUpScheduler tick triggered at IST now={}", now);

        String lockValue = UUID.randomUUID().toString();
        boolean acquired = false;
        try {
            acquired = lockService.tryLock(LOCK_KEY, lockValue, LOCK_TTL);
            log.info("TaskFollowUpScheduler lock acquisition status: acquired={}", acquired);
        } catch (Exception e) {
            log.error("Redis lock error during follow-up reminder check, proceeding without lock: {}", e.getMessage());
            acquired = true;
        }

        if (!acquired) {
            log.warn("Follow-up reminder lock '{}' is currently held by another instance. Skipping tick.", LOCK_KEY);
            return;
        }

        try {
            List<Task> dueTasks = taskRepository.findTasksDueForReminder(now, PageRequest.of(0, 100));
            log.info("TaskFollowUpScheduler queried findTasksDueForReminder(now={}): found {} task(s)", now, dueTasks.size());

            if (dueTasks.isEmpty()) {
                List<Task> upcoming24h = taskRepository.findTasksDueForReminder(now.plusHours(24), PageRequest.of(0, 100));
                if (!upcoming24h.isEmpty()) {
                    log.info("DIAGNOSTIC: Found {} task(s) due within next 24h (dueDate <= {}). First task id={}, dueDate={}, reminderSent={}",
                            upcoming24h.size(), now.plusHours(24), upcoming24h.get(0).getId(), upcoming24h.get(0).getDueDate(), upcoming24h.get(0).isReminderSent());
                }
            }

            for (Task task : dueTasks) {
                try {
                    log.info("Processing task reminder for taskId={}, title='{}', dueDate={}", task.getId(), task.getTitle(), task.getDueDate());
                    processTaskReminder(task);
                } catch (Exception e) {
                    log.error("Failed to process follow-up reminder for taskId={}", task.getId(), e);
                }
            }
        } finally {
            try {
                lockService.unlock(LOCK_KEY, lockValue);
            } catch (Exception e) {
                log.warn("Failed to release Redis lock '{}': {}", LOCK_KEY, e.getMessage());
            }
        }
    }

    @Transactional
    public void processTaskReminder(Task task) {
        int updatedCount = taskRepository.markReminderSent(task.getId());
        log.info("markReminderSent returned updatedCount={} for taskId={}", updatedCount, task.getId());
        if (updatedCount == 1) {
            UUID orgId = task.getOrganization() != null ? task.getOrganization().getId() : null;
            UUID assignedToId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;

            if (assignedToId != null) {
                TaskFollowUpDue event = new TaskFollowUpDue(orgId, task.getId(), assignedToId);
                outboxEventPublisher.publish(event);
                log.info("Published TaskFollowUpDue event for taskId={}, organizationId={}, assignedTo={}", task.getId(), orgId, assignedToId);
                meterRegistry.counter("reminders.processed").increment();
            } else {
                log.warn("Task taskId={} is due for reminder but assignedTo is null", task.getId());
            }
        } else {
            log.info("Task reminder already processed or markReminderSent returned 0 for taskId={}", task.getId());
        }
    }
}
