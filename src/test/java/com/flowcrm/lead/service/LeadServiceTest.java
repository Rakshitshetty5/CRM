package com.flowcrm.lead.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.auth.security.UserPrincipal;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.common.enums.ActivityType;
import com.flowcrm.common.enums.LeadSource;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.lead.dto.CreateLeadRequest;
import com.flowcrm.lead.dto.LeadActivityResponse;
import com.flowcrm.lead.dto.LeadResponse;
import com.flowcrm.lead.dto.UpdateLeadRequest;
import com.flowcrm.lead.dto.UpdateLeadStatusRequest;
import com.flowcrm.lead.entity.Lead;
import com.flowcrm.lead.entity.LeadActivity;
import com.flowcrm.lead.repository.LeadActivityRepository;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.organization.entity.Organization;
import com.flowcrm.outbox.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadActivityRepository leadActivityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserContext userContext;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private LeadServiceImpl leadService;

    private User authUser;
    private Lead testLead;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("FlowCRM")
                .slug("flowcrm")
                .build();

        authUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Admin")
                .lastName("User")
                .email("admin@flowcrm.com")
                .password("password")
                .role(Role.ADMIN)
                .active(true)
                .organization(testOrg)
                .build();

        testLead = Lead.builder()
                .id(UUID.randomUUID())
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@abc.com")
                .phone("+919876543210")
                .company("ABC Technologies")
                .status(LeadStatus.NEW)
                .source(LeadSource.WEBSITE)
                .notes("Interested in enterprise plan")
                .assignedTo(authUser)
                .organization(testOrg)
                .build();

        when(userContext.getUserId()).thenReturn(authUser.getId());

        UserPrincipal principal = new UserPrincipal(authUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Create Lead - Success setting assignedTo to authenticated user")
    void createLead_Success() {
        CreateLeadRequest request = new CreateLeadRequest(
                "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Technologies", LeadSource.WEBSITE, "Interested in enterprise plan"
        );

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.save(any(Lead.class))).thenReturn(testLead);

        LeadResponse response = leadService.createLead(request);

        assertNotNull(response);
        assertEquals(testLead.getId(), response.id());
        assertEquals(LeadStatus.NEW, response.status());
        assertEquals(authUser.getId(), response.assignedTo());

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(leadCaptor.capture());
        Lead savedLead = leadCaptor.getValue();
        assertEquals(authUser.getId(), savedLead.getAssignedTo().getId());

        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(leadActivityRepository).save(activityCaptor.capture());
        LeadActivity savedActivity = activityCaptor.getValue();

        assertEquals(ActivityType.LEAD_CREATED, savedActivity.getType());
        assertEquals("Lead created", savedActivity.getDescription());
        assertEquals(authUser.getId(), savedActivity.getPerformedBy().getId());
    }

    @Test
    @DisplayName("Get Leads - Success with pagination and filter")
    void getLeads_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Lead> leadPage = new PageImpl<>(List.of(testLead), pageable, 1);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(leadPage);


        Page<LeadResponse> result = leadService.getLeads(LeadStatus.NEW, authUser.getId(), "rahul", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testLead.getId(), result.getContent().get(0).id());
    }

    @Test
    @DisplayName("Get Lead By Id - Success")
    void getLeadById_Success() {
        UUID leadId = testLead.getId();
        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(leadId, testOrg.getId())).thenReturn(Optional.of(testLead));

        LeadResponse response = leadService.getLeadById(leadId);

        assertNotNull(response);
        assertEquals(leadId, response.id());
    }

    @Test
    @DisplayName("Get Lead By Id - Throws ResourceNotFoundException when lead not found")
    void getLeadById_NotFound() {
        UUID leadId = UUID.randomUUID();
        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(eq(leadId), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadService.getLeadById(leadId));
    }

    @Test
    @DisplayName("Update Lead - Success without changing assignedTo")
    void updateLead_Success() {
        UUID leadId = testLead.getId();
        UpdateLeadRequest request = new UpdateLeadRequest(
                "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech Updated", LeadSource.REFERRAL, "Updated notes"
        );

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(leadId, testOrg.getId())).thenReturn(Optional.of(testLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(testLead);

        LeadResponse response = leadService.updateLead(leadId, request);

        assertNotNull(response);
        assertEquals(LeadStatus.NEW, response.status());
        assertEquals(authUser.getId(), response.assignedTo());

        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(leadActivityRepository).save(activityCaptor.capture());
        LeadActivity savedActivity = activityCaptor.getValue();

        assertEquals(ActivityType.LEAD_UPDATED, savedActivity.getType());
        assertEquals("Lead updated", savedActivity.getDescription());
    }

    @Test
    @DisplayName("Update Lead Status - Success")
    void updateLeadStatus_Success() {
        UUID leadId = testLead.getId();
        UpdateLeadStatusRequest request = new UpdateLeadStatusRequest(LeadStatus.QUALIFIED);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(leadId, testOrg.getId())).thenReturn(Optional.of(testLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(testLead);

        LeadResponse response = leadService.updateLeadStatus(leadId, request);

        assertNotNull(response);
        assertEquals(LeadStatus.QUALIFIED, testLead.getStatus());

        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(leadActivityRepository).save(activityCaptor.capture());
        LeadActivity savedActivity = activityCaptor.getValue();

        assertEquals(ActivityType.STAGE_CHANGED, savedActivity.getType());
        assertTrue(savedActivity.getDescription().contains("NEW"));
        assertTrue(savedActivity.getDescription().contains("QUALIFIED"));
    }

    @Test
    @DisplayName("Get Lead Activities - Success")
    void getLeadActivities_Success() {
        UUID leadId = testLead.getId();
        LeadActivity activity = LeadActivity.builder()
                .id(UUID.randomUUID())
                .lead(testLead)
                .type(ActivityType.LEAD_CREATED)
                .description("Lead created")
                .performedBy(authUser)
                .build();

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(leadId, testOrg.getId())).thenReturn(Optional.of(testLead));
        when(leadActivityRepository.findByLeadIdOrderByCreatedAtDesc(leadId)).thenReturn(List.of(activity));

        List<LeadActivityResponse> response = leadService.getLeadActivities(leadId);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(ActivityType.LEAD_CREATED, response.get(0).type());
        assertEquals(authUser.getId(), response.get(0).performedBy());
    }

    @Test
    @DisplayName("Assign Lead - Sales Rep can reassign lead assigned to them")
    void assignLead_SalesRepSuccess() {
        User salesRepA = User.builder()
                .id(UUID.randomUUID())
                .firstName("RepA")
                .lastName("User")
                .email("repa@flowcrm.com")
                .role(Role.SALES_REP)
                .active(true)
                .organization(testOrg)
                .build();

        User salesRepB = User.builder()
                .id(UUID.randomUUID())
                .firstName("RepB")
                .lastName("User")
                .email("repb@flowcrm.com")
                .role(Role.SALES_REP)
                .active(true)
                .organization(testOrg)
                .build();

        Lead leadAssignedToA = Lead.builder()
                .id(UUID.randomUUID())
                .firstName("Test")
                .lastName("Lead")
                .status(LeadStatus.NEW)
                .assignedTo(salesRepA)
                .organization(testOrg)
                .build();

        when(userContext.getUserId()).thenReturn(salesRepA.getId());
        when(userRepository.findById(salesRepA.getId())).thenReturn(Optional.of(salesRepA));
        when(leadRepository.findByIdAndOrganizationId(leadAssignedToA.getId(), testOrg.getId())).thenReturn(Optional.of(leadAssignedToA));
        when(userRepository.findByIdAndOrganizationId(salesRepB.getId(), testOrg.getId())).thenReturn(Optional.of(salesRepB));
        when(leadRepository.save(any(Lead.class))).thenReturn(leadAssignedToA);

        LeadResponse response = leadService.assignLead(leadAssignedToA.getId(), new com.flowcrm.lead.dto.LeadAssignmentRequest(salesRepB.getId()));

        assertNotNull(response);
        verify(leadActivityRepository).save(any(LeadActivity.class));
    }

    @Test
    @DisplayName("Assign Lead - Sales Rep cannot reassign lead NOT assigned to them")
    void assignLead_SalesRepDenied() {
        User salesRepA = User.builder()
                .id(UUID.randomUUID())
                .firstName("RepA")
                .lastName("User")
                .email("repa@flowcrm.com")
                .role(Role.SALES_REP)
                .active(true)
                .organization(testOrg)
                .build();

        User salesRepB = User.builder()
                .id(UUID.randomUUID())
                .firstName("RepB")
                .lastName("User")
                .email("repb@flowcrm.com")
                .role(Role.SALES_REP)
                .active(true)
                .organization(testOrg)
                .build();

        Lead leadAssignedToB = Lead.builder()
                .id(UUID.randomUUID())
                .firstName("Test")
                .lastName("Lead")
                .status(LeadStatus.NEW)
                .assignedTo(salesRepB)
                .organization(testOrg)
                .build();

        when(userContext.getUserId()).thenReturn(salesRepA.getId());
        when(userRepository.findById(salesRepA.getId())).thenReturn(Optional.of(salesRepA));
        when(leadRepository.findByIdAndOrganizationId(leadAssignedToB.getId(), testOrg.getId())).thenReturn(Optional.of(leadAssignedToB));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> leadService.assignLead(leadAssignedToB.getId(), new com.flowcrm.lead.dto.LeadAssignmentRequest(salesRepA.getId())));
    }
}
