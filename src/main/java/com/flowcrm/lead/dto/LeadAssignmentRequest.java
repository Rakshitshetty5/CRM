package com.flowcrm.lead.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeadAssignmentRequest(

        @NotNull(message = "Assigned user is required")
        UUID assignedTo
) {
}
