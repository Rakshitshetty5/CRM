package com.flowcrm.task.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.common.enums.ActivityType;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.common.event.TaskAssignedEvent;
import com.flowcrm.common.event.TaskCreatedEvent;
import com.flowcrm.common.event.TaskStatusChangedEvent;
import com.flowcrm.outbox.OutboxEventPublisher;
import com.flowcrm.lead.entity.Lead;
import com.flowcrm.lead.entity.LeadActivity;
import com.flowcrm.lead.repository.LeadActivityRepository;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.task.dto.CreateTaskRequest;
import com.flowcrm.task.dto.TaskResponse;
import com.flowcrm.task.dto.UpdateTaskRequest;
import com.flowcrm.task.dto.UpdateTaskStatusRequest;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
import com.flowcrm.task.repository.TaskSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final UserContext userContext;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Lead lead = leadRepository.findByIdAndOrganizationId(request.leadId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + request.leadId()));

        User assignedUser = userRepository.findByIdAndOrganizationId(request.assignedTo(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.assignedTo()));

        validateTaskAssignee(assignedUser);

        if (request.dueDate() != null && request.dueDate().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(TaskStatus.PENDING)
                .priority(request.priority())
                .dueDate(request.dueDate())
                .lead(lead)
                .assignedTo(assignedUser)
                .organization(currentUser.getOrganization())
                .build();

        task.setCreatedBy(currentUser.getId());

        Task savedTask = taskRepository.save(task);

        outboxEventPublisher.publish(new TaskCreatedEvent(
                savedTask.getOrganization().getId(),
                savedTask.getId(),
                savedTask.getLead() != null ? savedTask.getLead().getId() : null,
                savedTask.getAssignedTo() != null ? savedTask.getAssignedTo().getId() : null
        ));

        return mapToTaskResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(TaskStatus status, TaskPriority priority, UUID assignedTo, UUID leadId, Pageable pageable) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        Specification<Task> spec = TaskSpecification.filterTasks(organizationId, currentUser.getId(), isAdmin, status, priority, assignedTo, leadId);
        return taskRepository.findAll(spec, pageable).map(this::mapToTaskResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Task task = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        enforceTaskAccess(task);

        return mapToTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Task task = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        enforceTaskOwnership(task, currentUser);

        User assignedUser = userRepository.findByIdAndOrganizationId(request.assignedTo(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.assignedTo()));

        validateTaskAssignee(assignedUser);

        User oldAssignee = task.getAssignedTo();
        UUID oldAssigneeId = oldAssignee != null ? oldAssignee.getId() : null;

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setAssignedTo(assignedUser);

        Task updatedTask = taskRepository.save(task);

        if (!assignedUser.getId().equals(oldAssigneeId)) {
            outboxEventPublisher.publish(new TaskAssignedEvent(
                    updatedTask.getOrganization().getId(),
                    updatedTask.getId(),
                    assignedUser.getId()
            ));
        }

        return mapToTaskResponse(updatedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(UUID taskId, UpdateTaskStatusRequest request) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Task task = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        enforceTaskOwnership(task, currentUser);

        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus = request.status();

        if (oldStatus == newStatus) {
            return mapToTaskResponse(task);
        }

        task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);

        if (newStatus == TaskStatus.COMPLETED) {
            LeadActivity activity = LeadActivity.builder()
                    .lead(updatedTask.getLead())
                    .type(ActivityType.TASK_COMPLETED)
                    .description("Task completed: " + updatedTask.getTitle())
                    .performedBy(currentUser)
                    .build();

            leadActivityRepository.save(activity);
        }

        outboxEventPublisher.publish(new TaskStatusChangedEvent(
                updatedTask.getOrganization().getId(),
                updatedTask.getId(),
                oldStatus,
                newStatus
        ));

        return mapToTaskResponse(updatedTask);
    }

    /**
     * Validates that the target user is a valid task assignee:
     * must be an active user with SALES_REP role.
     */
    private void validateTaskAssignee(User assignee) {
        if (assignee.getRole() != Role.SALES_REP) {
            throw new IllegalArgumentException("Task can only be assigned to a user with SALES_REP role");
        }
        if (!assignee.isActive()) {
            throw new IllegalArgumentException("Cannot assign task to an inactive user");
        }
    }

    /**
     * Enforces that a SALES_REP can view a task if assignedTo OR createdBy == currentUser.
     * ADMIN can access any task in their organization.
     */
    private void enforceTaskAccess(Task task) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.SALES_REP) {
            boolean isAssignee = task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentUser.getId());
            boolean isCreator = task.getCreatedBy() != null && task.getCreatedBy().equals(currentUser.getId());
            if (!isAssignee && !isCreator) {
                throw new AccessDeniedException("You do not have access to this task");
            }
        }
    }

    /**
     * Enforces that a SALES_REP can only modify tasks assigned to them.
     * ADMIN can modify any task.
     */
    private void enforceTaskOwnership(Task task, User currentUser) {
        if (currentUser.getRole() == Role.SALES_REP) {
            if (!task.getAssignedTo().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have access to this task");
            }
        }
    }

    private User getCurrentUser() {
        UUID userId = userContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getLead() != null ? task.getLead().getId() : null,
                task.getAssignedTo() != null ? task.getAssignedTo().getId() : null,
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
