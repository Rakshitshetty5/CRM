package com.flowcrm.lead.controller;

import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.lead.dto.CreateLeadRequest;
import com.flowcrm.lead.dto.LeadActivityResponse;
import com.flowcrm.lead.dto.LeadAssignmentRequest;
import com.flowcrm.lead.dto.LeadResponse;
import com.flowcrm.lead.dto.UpdateLeadRequest;
import com.flowcrm.lead.dto.UpdateLeadStatusRequest;
import com.flowcrm.lead.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Lead management APIs")
public class LeadController {

    private final LeadService leadService;

    @Operation(summary = "Create lead", description = "Creates a new lead within the organization")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lead created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody CreateLeadRequest request) {
        LeadResponse response = leadService.createLead(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get leads", description = "Retrieves a paged, filtered list of leads within the organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leads retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<LeadResponse>> getLeads(
            @Parameter(description = "Filter by lead status") @RequestParam(required = false) LeadStatus status,
            @Parameter(description = "Filter by assigned user ID") @RequestParam(required = false) UUID assignedTo,
            @Parameter(description = "Search term for name, email, or company") @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeadResponse> response = leadService.getLeads(status, assignedTo, search, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get lead by ID", description = "Retrieves a lead by ID within the organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lead retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    @GetMapping("/{leadId}")
    public ResponseEntity<LeadResponse> getLeadById(@Parameter(description = "UUID of the lead") @PathVariable UUID leadId) {
        LeadResponse response = leadService.getLeadById(leadId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update lead", description = "Updates an existing lead details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lead updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    @PutMapping("/{leadId}")
    public ResponseEntity<LeadResponse> updateLead(
            @Parameter(description = "UUID of the lead") @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadRequest request
    ) {
        LeadResponse response = leadService.updateLead(leadId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update lead status", description = "Updates the status of a lead and logs status change activity")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lead status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    @PatchMapping("/{leadId}/status")
    public ResponseEntity<LeadResponse> updateLeadStatus(
            @Parameter(description = "UUID of the lead") @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadStatusRequest request
    ) {
        LeadResponse response = leadService.updateLeadStatus(leadId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get lead activities", description = "Retrieves activity audit trail for a lead")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lead activities retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    @GetMapping("/{leadId}/activities")
    public ResponseEntity<List<LeadActivityResponse>> getLeadActivities(@Parameter(description = "UUID of the lead") @PathVariable UUID leadId) {
        List<LeadActivityResponse> response = leadService.getLeadActivities(leadId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Assign lead", description = "Assigns a lead to a user and emits a LeadAssigned event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lead assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Lead or user not found")
    })
    @PatchMapping("/{leadId}/assignment")
    public ResponseEntity<LeadResponse> assignLead(
            @Parameter(description = "UUID of the lead") @PathVariable UUID leadId,
            @Valid @RequestBody LeadAssignmentRequest request
    ) {
        LeadResponse response = leadService.assignLead(leadId, request);
        return ResponseEntity.ok(response);
    }
}


