package com.flowcrm.lead.service;

import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.lead.dto.CreateLeadRequest;
import com.flowcrm.lead.dto.LeadActivityResponse;
import com.flowcrm.lead.dto.LeadResponse;
import com.flowcrm.lead.dto.UpdateLeadRequest;
import com.flowcrm.lead.dto.UpdateLeadStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LeadService {
    LeadResponse createLead(CreateLeadRequest request);
    Page<LeadResponse> getLeads(LeadStatus status, UUID assignedTo, String search, Pageable pageable);
    LeadResponse getLeadById(UUID leadId);
    LeadResponse updateLead(UUID leadId, UpdateLeadRequest request);
    LeadResponse updateLeadStatus(UUID leadId, UpdateLeadStatusRequest request);
    List<LeadActivityResponse> getLeadActivities(UUID leadId);
}
