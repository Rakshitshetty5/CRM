package com.flowcrm.task.dto;

import com.flowcrm.common.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(

        @NotNull(message = "Task status is required")
        TaskStatus status
) {
}