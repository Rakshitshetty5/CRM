package com.flowcrm.lead.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.common.enums.ActivityType;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.lead.dto.CreateLeadRequest;
import com.flowcrm.lead.dto.LeadActivityResponse;
import com.flowcrm.lead.dto.LeadAssignmentRequest;
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
import org.springframework.security.access.AccessDeniedException;
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
                .organization(currentUser.getOrganization())
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
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        // SALES_REP: force assignedTo to their own ID, ignore any client-supplied value
        if (currentUser.getRole() == Role.SALES_REP) {
            assignedTo = currentUser.getId();
        }

        Specification<Lead> spec = LeadSpecification.filterLeads(organizationId, status, assignedTo, search);
        Page<Lead> leadsPage = leadRepository.findAll(spec, pageable);
        return leadsPage.map(this::mapToLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getLeadById(UUID leadId) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Lead lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        enforceLeadAccess(lead);

        return mapToLeadResponse(lead);
    }

    @Override
    @Transactional
    public LeadResponse updateLead(UUID leadId, UpdateLeadRequest request) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Lead lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        enforceLeadOwnership(lead, currentUser);

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
                .performedBy(currentUser)
                .build();

        leadActivityRepository.save(activity);

        return mapToLeadResponse(updatedLead);
    }

    @Override
    @Transactional
    public LeadResponse updateLeadStatus(UUID leadId, UpdateLeadStatusRequest request) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Lead lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        enforceLeadOwnership(lead, currentUser);

        LeadStatus oldStatus = lead.getStatus();
        LeadStatus newStatus = request.status();

        if (oldStatus == newStatus) {
            return mapToLeadResponse(lead);
        }

        lead.setStatus(newStatus);
        Lead updatedLead = leadRepository.save(lead);

        LeadActivity activity = LeadActivity.builder()
                .lead(updatedLead)
                .type(ActivityType.STAGE_CHANGED)
                .description("Status changed from " + oldStatus + " to " + newStatus)
                .performedBy(currentUser)
                .build();

        leadActivityRepository.save(activity);

        return mapToLeadResponse(updatedLead);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadActivityResponse> getLeadActivities(UUID leadId) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Lead lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        enforceLeadAccess(lead);

        List<LeadActivity> activities = leadActivityRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
        return activities.stream()
                .map(this::mapToLeadActivityResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeadResponse assignLead(UUID leadId, LeadAssignmentRequest request) {
        User currentUser = getCurrentUser();
        UUID organizationId = currentUser.getOrganization().getId();

        Lead lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        enforceLeadOwnership(lead, currentUser);

        User assignee = userRepository.findByIdAndOrganizationId(request.assignedTo(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.assignedTo()));

        if (assignee.getRole() != Role.SALES_REP) {
            throw new IllegalArgumentException("Lead can only be assigned to a user with SALES_REP role");
        }

        if (!assignee.isActive()) {
            throw new IllegalArgumentException("Cannot assign lead to an inactive user");
        }

        if (lead.getAssignedTo() != null && lead.getAssignedTo().getId().equals(assignee.getId())) {
            return mapToLeadResponse(lead);
        }

        lead.setAssignedTo(assignee);
        Lead updatedLead = leadRepository.save(lead);

        LeadActivity activity = LeadActivity.builder()
                .lead(updatedLead)
                .type(ActivityType.LEAD_ASSIGNED)
                .description("Lead assigned to " + assignee.getFirstName() + " " + assignee.getLastName())
                .performedBy(currentUser)
                .build();

        leadActivityRepository.save(activity);

        return mapToLeadResponse(updatedLead);
    }

    /**
     * Enforces that a SALES_REP can only access leads assigned to them.
     * ADMIN can access any lead.
     */
    private void enforceLeadAccess(Lead lead) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.SALES_REP) {
            if (!lead.getAssignedTo().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have access to this lead");
            }
        }
    }

    /**
     * Enforces that a SALES_REP can only modify leads assigned to them.
     * ADMIN can modify any lead.
     */
    private void enforceLeadOwnership(Lead lead, User currentUser) {
        if (currentUser.getRole() == Role.SALES_REP) {
            if (!lead.getAssignedTo().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have access to this lead");
            }
        }
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
