package com.flowcrm.common.event;

import com.flowcrm.common.enums.LeadStatus;

import java.time.Instant;
import java.util.UUID;

public record LeadStatusChangedEvent(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID entityId,
        Instant occurredAt,
        UUID leadId,
        LeadStatus oldStatus,
        LeadStatus newStatus
) implements DomainEvent {

    public LeadStatusChangedEvent(UUID organizationId, UUID leadId, LeadStatus oldStatus, LeadStatus newStatus) {
        this(UUID.randomUUID(), "LeadStatusChanged", organizationId, leadId, Instant.now(), leadId, oldStatus, newStatus);
    }
}
