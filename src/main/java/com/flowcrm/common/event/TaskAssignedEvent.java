package com.flowcrm.common.event;

import java.time.Instant;
import java.util.UUID;

public record TaskAssignedEvent(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID entityId,
        Instant occurredAt,
        UUID taskId,
        UUID assignedTo
) implements DomainEvent {

    public TaskAssignedEvent(UUID organizationId, UUID taskId, UUID assignedTo) {
        this(UUID.randomUUID(), "TaskAssigned", organizationId, taskId, Instant.now(), taskId, assignedTo);
    }
}
