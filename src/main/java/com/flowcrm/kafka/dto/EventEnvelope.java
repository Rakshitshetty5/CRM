package com.flowcrm.kafka.dto;

import java.util.UUID;

public record EventEnvelope(
        UUID id,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String payload
) {}
