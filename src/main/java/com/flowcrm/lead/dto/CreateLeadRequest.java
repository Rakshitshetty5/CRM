package com.flowcrm.lead.dto;

import com.flowcrm.common.enums.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLeadRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 150, message = "Company must not exceed 150 characters")
        String company,

        @NotNull(message = "Lead source is required")
        LeadSource source,

        @NotNull(message = "Assigned user is required")
        UUID assignedTo,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}