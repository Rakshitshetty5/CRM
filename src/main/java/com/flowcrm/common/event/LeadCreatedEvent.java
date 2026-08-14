package com.flowcrm.common.event;

import java.time.Instant;
import java.util.UUID;

public record LeadCreatedEvent(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID entityId,
        Instant occurredAt,
        UUID leadId,
        UUID assignedTo
) implements DomainEvent {

    public LeadCreatedEvent(UUID organizationId, UUID leadId, UUID assignedTo) {
        this(UUID.randomUUID(), "LeadCreated", organizationId, leadId, Instant.now(), leadId, assignedTo);
    }
}
