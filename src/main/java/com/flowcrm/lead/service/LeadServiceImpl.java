package com.flowcrm.lead.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.common.enums.ActivityType;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.lead.dto.CreateLeadRequest;
import com.flowcrm.lead.dto.LeadActivityResponse;
import com.flowcrm.lead.dto.LeadResponse;
import com.flowcrm.lead.dto.UpdateLeadRequest;
import com.flowcrm.lead.dto.UpdateLeadStatusRequest;
import com.flowcrm.lead.entity.Lead;
import com.flowcrm.lead.entity.LeadActivity;
import com.flowcrm.lead.repository.LeadActivityRepository;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.lead.repository.LeadSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final UserRepository userRepository;
    private final UserContext userContext;

    @Override
    @Transactional
    public LeadResponse createLead(CreateLeadRequest request) {
        User currentUser = getCurrentUser();

        Lead lead = Lead.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .company(request.company())
                .status(LeadStatus.NEW)
                .source(request.source())
                .notes(request.notes())
                .assignedTo(currentUser)
                .build();

        Lead savedLead = leadRepository.save(lead);

        LeadActivity activity = LeadActivity.builder()
                .lead(savedLead)
                .type(ActivityType.LEAD_CREATED)
                .description("Lead created")
                .performedBy(currentUser)
                .build();

        leadActivityRepository.save(activity);

        return mapToLeadResponse(savedLead);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> getLeads(LeadStatus status, UUID assignedTo, String search, Pageable pageable) {
        Specification<Lead> spec = LeadSpecification.filterLeads(status, assignedTo, search);
        Page<Lead> leadsPage = leadRepository.findAll(spec, pageable);
        return leadsPage.map(this::mapToLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getLeadById(UUID leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));
        return mapToLeadResponse(lead);
    }

    @Override
    @Transactional
    public LeadResponse updateLead(UUID leadId, UpdateLeadRequest request) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        lead.setFirstName(request.firstName());
        lead.setLastName(request.lastName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompany(request.company());
        lead.setSource(request.source());
        lead.setNotes(request.notes());

        Lead updatedLead = leadRepository.save(lead);

        LeadActivity activity = LeadActivity.builder()
                .lead(updatedLead)
                .type(ActivityType.LEAD_UPDATED)
                .description("Lead updated")
                .performedBy(getCurrentUser())
                .build();

        leadActivityRepository.save(activity);

        return mapToLeadResponse(updatedLead);
    }

    @Override
    @Transactional
    public LeadResponse updateLeadStatus(UUID leadId, UpdateLeadStatusRequest request) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        LeadStatus oldStatus = lead.getStatus();
        LeadStatus newStatus = request.status();

        lead.setStatus(newStatus);
        Lead updatedLead = leadRepository.save(lead);

        LeadActivity activity = LeadActivity.builder()
                .lead(updatedLead)
                .type(ActivityType.STAGE_CHANGED)
                .description("Status changed from " + oldStatus + " to " + newStatus)
                .performedBy(getCurrentUser())
                .build();

        leadActivityRepository.save(activity);

        return mapToLeadResponse(updatedLead);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadActivityResponse> getLeadActivities(UUID leadId) {
        if (!leadRepository.existsById(leadId)) {
            throw new ResourceNotFoundException("Lead not found with id: " + leadId);
        }

        List<LeadActivity> activities = leadActivityRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
        return activities.stream()
                .map(this::mapToLeadActivityResponse)
                .toList();
    }

    private User getCurrentUser() {
        UUID userId = userContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private LeadResponse mapToLeadResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getFirstName(),
                lead.getLastName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getCompany(),
                lead.getStatus(),
                lead.getSource(),
                lead.getNotes(),
                lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null,
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    private LeadActivityResponse mapToLeadActivityResponse(LeadActivity activity) {
        return new LeadActivityResponse(
                activity.getId(),
                activity.getType(),
                activity.getDescription(),
                activity.getPerformedBy() != null ? activity.getPerformedBy().getId() : null,
                activity.getCreatedAt()
        );
    }
}
