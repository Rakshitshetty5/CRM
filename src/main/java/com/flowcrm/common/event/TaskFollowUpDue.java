package com.flowcrm.common.event;

import java.time.Instant;
import java.util.UUID;

public record TaskFollowUpDue(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID entityId,
        Instant occurredAt,
        UUID taskId,
        UUID assignedTo
) implements DomainEvent {

    public TaskFollowUpDue(UUID organizationId, UUID taskId, UUID assignedTo) {
        this(UUID.randomUUID(), "TaskFollowUpDue", organizationId, taskId, Instant.now(), taskId, assignedTo);
    }
}
