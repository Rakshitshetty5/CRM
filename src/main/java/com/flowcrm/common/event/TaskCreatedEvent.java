package com.flowcrm.common.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCreatedEvent(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID entityId,
        Instant occurredAt,
        UUID taskId,
        UUID leadId,
        UUID assignedTo
) implements DomainEvent {

    public TaskCreatedEvent(UUID organizationId, UUID taskId, UUID leadId, UUID assignedTo) {
        this(UUID.randomUUID(), "TaskCreated", organizationId, taskId, Instant.now(), taskId, leadId, assignedTo);
    }
}
