package com.flowcrm.common.event;

import com.flowcrm.common.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskStatusChangedEvent(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID entityId,
        Instant occurredAt,
        UUID taskId,
        TaskStatus oldStatus,
        TaskStatus newStatus
) implements DomainEvent {

    public TaskStatusChangedEvent(UUID organizationId, UUID taskId, TaskStatus oldStatus, TaskStatus newStatus) {
        this(UUID.randomUUID(), "TaskStatusChanged", organizationId, taskId, Instant.now(), taskId, oldStatus, newStatus);
    }
}
