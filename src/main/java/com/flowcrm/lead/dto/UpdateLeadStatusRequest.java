package com.flowcrm.lead.dto;

import com.flowcrm.common.enums.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLeadStatusRequest(

        @NotNull(message = "Lead status is required")
        LeadStatus status
) {
}