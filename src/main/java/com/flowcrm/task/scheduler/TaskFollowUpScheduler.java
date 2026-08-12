package com.flowcrm.task.scheduler;

import com.flowcrm.common.event.TaskFollowUpDue;
import com.flowcrm.outbox.OutboxEventPublisher;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskFollowUpScheduler {

    private final TaskRepository taskRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(fixedDelayString = "${app.follow-up.polling.fixed-delay:60000}")
    public void checkForDueTasks() {
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
                log.info("Published TaskFollowUpDue event for taskId={}, assignedTo={}", task.getId(), assignedToId);
            }
        } else {
            log.debug("Task reminder already processed by another instance for taskId={}", task.getId());
        }
    }
}
