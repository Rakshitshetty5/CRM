package com.flowcrm.common.event;

import java.time.Instant;
import java.util.UUID;

public record LeadUpdatedEvent(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID entityId,
        Instant occurredAt,
        UUID leadId
) implements DomainEvent {

    public LeadUpdatedEvent(UUID organizationId, UUID leadId) {
        this(UUID.randomUUID(), "LeadUpdated", organizationId, leadId, Instant.now(), leadId);
    }
}
