package com.flowcrm.task.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.common.enums.ActivityType;
import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
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

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Lead lead = leadRepository.findById(request.leadId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + request.leadId()));

        User assignedUser = userRepository.findById(request.assignedTo())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.assignedTo()));

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(TaskStatus.PENDING)
                .priority(request.priority())
                .dueDate(request.dueDate())
                .lead(lead)
                .assignedTo(assignedUser)
                .build();

        Task savedTask = taskRepository.save(task);
        return mapToTaskResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(TaskStatus status, TaskPriority priority, UUID assignedTo, UUID leadId, Pageable pageable) {
        Specification<Task> spec = TaskSpecification.filterTasks(status, priority, assignedTo, leadId);
        return taskRepository.findAll(spec, pageable).map(this::mapToTaskResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        return mapToTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User assignedUser = userRepository.findById(request.assignedTo())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.assignedTo()));

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setAssignedTo(assignedUser);

        Task updatedTask = taskRepository.save(task);
        return mapToTaskResponse(updatedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(UUID taskId, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        task.setStatus(request.status());
        Task updatedTask = taskRepository.save(task);

        if (request.status() == TaskStatus.COMPLETED) {
            User currentUser = getCurrentUser();

            LeadActivity activity = LeadActivity.builder()
                    .lead(updatedTask.getLead())
                    .type(ActivityType.TASK_COMPLETED)
                    .description("Task completed: " + updatedTask.getTitle())
                    .performedBy(currentUser)
                    .build();

            leadActivityRepository.save(activity);
        }

        return mapToTaskResponse(updatedTask);
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
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
