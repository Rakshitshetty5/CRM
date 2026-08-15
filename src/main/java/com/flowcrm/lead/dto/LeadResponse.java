package com.flowcrm.lead.dto;

import com.flowcrm.common.enums.LeadSource;
import com.flowcrm.common.enums.LeadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String company,
        LeadStatus status,
        LeadSource source,
        String notes,
        UUID assignedTo,
        String assignedToName,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}