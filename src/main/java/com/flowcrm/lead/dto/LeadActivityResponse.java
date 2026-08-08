package com.flowcrm.lead.dto;

import com.flowcrm.common.enums.ActivityType;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadActivityResponse(
        UUID id,
        ActivityType type,
        String description,
        UUID performedBy,
        LocalDateTime createdAt
) {
}