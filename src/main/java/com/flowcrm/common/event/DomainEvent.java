package com.flowcrm.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    String eventType();
    UUID organizationId();
    UUID entityId();
    Instant occurredAt();
}
