package com.flowcrm.task.scheduler;

import com.flowcrm.auth.entity.User;
import com.flowcrm.common.event.TaskFollowUpDue;
import com.flowcrm.organization.entity.Organization;
import com.flowcrm.outbox.OutboxEventPublisher;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskFollowUpSchedulerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private TaskFollowUpScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TaskFollowUpScheduler(taskRepository, outboxEventPublisher);
    }

    @Test
    void testCheckForDueTasksPublishesEventWhenTaskIsClaimed() {
        UUID taskId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Organization org = Organization.builder().id(orgId).build();
        User user = User.builder().id(userId).build();
        Task task = Task.builder()
                .id(taskId)
                .organization(org)
                .assignedTo(user)
                .dueDate(LocalDateTime.now().minusMinutes(5))
                .reminderSent(false)
                .build();

        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(task));
        when(taskRepository.markReminderSent(taskId)).thenReturn(1);

        scheduler.checkForDueTasks();

        verify(taskRepository, times(1)).markReminderSent(taskId);
        ArgumentCaptor<TaskFollowUpDue> captor = ArgumentCaptor.forClass(TaskFollowUpDue.class);
        verify(outboxEventPublisher, times(1)).publish(captor.capture());

        TaskFollowUpDue event = captor.getValue();
        assertNotNull(event);
        assertEquals(taskId, event.taskId());
        assertEquals(orgId, event.organizationId());
        assertEquals(userId, event.assignedTo());
        assertEquals("TaskFollowUpDue", event.eventType());
    }

    @Test
    void testCheckForDueTasksSkipsWhenTaskClaimedByAnotherInstance() {
        UUID taskId = UUID.randomUUID();
        Organization org = Organization.builder().id(UUID.randomUUID()).build();
        User user = User.builder().id(UUID.randomUUID()).build();
        Task task = Task.builder()
                .id(taskId)
                .organization(org)
                .assignedTo(user)
                .dueDate(LocalDateTime.now().minusMinutes(5))
                .reminderSent(false)
                .build();

        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(task));
        when(taskRepository.markReminderSent(taskId)).thenReturn(0);

        scheduler.checkForDueTasks();

        verify(taskRepository, times(1)).markReminderSent(taskId);
        verifyNoInteractions(outboxEventPublisher);
    }

    @Test
    void testProcessTaskReminderHandlesExceptionGracefully() {
        UUID taskId1 = UUID.randomUUID();
        UUID taskId2 = UUID.randomUUID();
        Organization org = Organization.builder().id(UUID.randomUUID()).build();
        User user = User.builder().id(UUID.randomUUID()).build();

        Task task1 = Task.builder().id(taskId1).organization(org).assignedTo(user).build();
        Task task2 = Task.builder().id(taskId2).organization(org).assignedTo(user).build();

        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(task1, task2));
        when(taskRepository.markReminderSent(taskId1)).thenThrow(new RuntimeException("DB error"));
        when(taskRepository.markReminderSent(taskId2)).thenReturn(1);

        scheduler.checkForDueTasks();

        verify(taskRepository, times(1)).markReminderSent(taskId1);
        verify(taskRepository, times(1)).markReminderSent(taskId2);
        verify(outboxEventPublisher, times(1)).publish(any());
    }
}
