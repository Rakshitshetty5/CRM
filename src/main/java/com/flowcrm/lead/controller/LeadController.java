package com.flowcrm.lead.controller;

import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.lead.dto.CreateLeadRequest;
import com.flowcrm.lead.dto.LeadActivityResponse;
import com.flowcrm.lead.dto.LeadAssignmentRequest;
import com.flowcrm.lead.dto.LeadResponse;
import com.flowcrm.lead.dto.UpdateLeadRequest;
import com.flowcrm.lead.dto.UpdateLeadStatusRequest;
import com.flowcrm.lead.service.LeadService;
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
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody CreateLeadRequest request) {
        LeadResponse response = leadService.createLead(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<LeadResponse>> getLeads(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeadResponse> response = leadService.getLeads(status, assignedTo, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{leadId}")
    public ResponseEntity<LeadResponse> getLeadById(@PathVariable UUID leadId) {
        LeadResponse response = leadService.getLeadById(leadId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{leadId}")
    public ResponseEntity<LeadResponse> updateLead(
            @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadRequest request
    ) {
        LeadResponse response = leadService.updateLead(leadId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{leadId}/status")
    public ResponseEntity<LeadResponse> updateLeadStatus(
            @PathVariable UUID leadId,
            @Valid @RequestBody UpdateLeadStatusRequest request
    ) {
        LeadResponse response = leadService.updateLeadStatus(leadId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{leadId}/activities")
    public ResponseEntity<List<LeadActivityResponse>> getLeadActivities(@PathVariable UUID leadId) {
        List<LeadActivityResponse> response = leadService.getLeadActivities(leadId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{leadId}/assignment")
    public ResponseEntity<LeadResponse> assignLead(
            @PathVariable UUID leadId,
            @Valid @RequestBody LeadAssignmentRequest request
    ) {
        LeadResponse response = leadService.assignLead(leadId, request);
        return ResponseEntity.ok(response);
    }
}

