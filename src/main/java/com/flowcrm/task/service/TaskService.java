package com.flowcrm.task.service;

import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.task.dto.CreateTaskRequest;
import com.flowcrm.task.dto.TaskResponse;
import com.flowcrm.task.dto.UpdateTaskRequest;
import com.flowcrm.task.dto.UpdateTaskStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest request);
    Page<TaskResponse> getTasks(TaskStatus status, TaskPriority priority, UUID assignedTo, UUID leadId, Pageable pageable);
    TaskResponse getTaskById(UUID taskId);
    TaskResponse updateTask(UUID taskId, UpdateTaskRequest request);
    TaskResponse updateTaskStatus(UUID taskId, UpdateTaskStatusRequest request);
}
