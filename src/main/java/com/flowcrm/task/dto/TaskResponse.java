package com.flowcrm.task.dto;

import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueDate,
        UUID leadId,
        UUID assignedTo,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}