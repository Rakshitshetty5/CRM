package com.flowcrm.dashboard.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.dashboard.dto.DashboardSummaryResponse;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.organization.entity.Organization;
import com.flowcrm.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserContext userContext;

    private DashboardServiceImpl dashboardService;

    private Organization testOrg;
    private User adminUser;
    private User salesRepUser;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(leadRepository, taskRepository, userRepository, userContext);

        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Org")
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .role(Role.ADMIN)
                .organization(testOrg)
                .build();

        salesRepUser = User.builder()
                .id(UUID.randomUUID())
                .email("rep@test.com")
                .role(Role.SALES_REP)
                .organization(testOrg)
                .build();
    }

    @Test
    void testGetDashboardSummaryAdmin() {
        when(userContext.getUserId()).thenReturn(adminUser.getId());
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        when(leadRepository.countByOrganizationId(testOrg.getId())).thenReturn(10L);
        List<Object[]> adminStatusCounts = List.<Object[]>of(
                new Object[]{LeadStatus.NEW, 4L},
                new Object[]{LeadStatus.QUALIFIED, 6L}
        );
        when(leadRepository.countLeadsByStatusForAdmin(testOrg.getId())).thenReturn(adminStatusCounts);

        when(taskRepository.countByOrganizationId(testOrg.getId())).thenReturn(15L);
        when(taskRepository.countByOrganizationIdAndStatusNot(eq(testOrg.getId()), eq(TaskStatus.COMPLETED))).thenReturn(8L);
        when(taskRepository.countByOrganizationIdAndStatus(eq(testOrg.getId()), eq(TaskStatus.COMPLETED))).thenReturn(7L);
        when(taskRepository.countByOrganizationIdAndStatusNotAndDueDateBefore(eq(testOrg.getId()), eq(TaskStatus.COMPLETED), any())).thenReturn(2L);

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(10L, response.totalLeads());
        assertEquals(4L, response.leadsByStatus().get(LeadStatus.NEW));
        assertEquals(6L, response.leadsByStatus().get(LeadStatus.QUALIFIED));
        assertEquals(15L, response.totalTasks());
        assertEquals(8L, response.pendingTasks());
        assertEquals(7L, response.completedTasks());
        assertEquals(2L, response.overdueTasks());
    }

    @Test
    void testGetDashboardSummarySalesRep() {
        when(userContext.getUserId()).thenReturn(salesRepUser.getId());
        when(userRepository.findById(salesRepUser.getId())).thenReturn(Optional.of(salesRepUser));

        when(leadRepository.countByOrganizationIdAndAssignedToId(testOrg.getId(), salesRepUser.getId())).thenReturn(5L);
        List<Object[]> repStatusCounts = List.<Object[]>of(
                new Object[]{LeadStatus.NEW, 5L}
        );
        when(leadRepository.countLeadsByStatusForSalesRep(testOrg.getId(), salesRepUser.getId())).thenReturn(repStatusCounts);

        when(taskRepository.countTasksForSalesRep(testOrg.getId(), salesRepUser.getId())).thenReturn(6L);
        when(taskRepository.countTasksForSalesRepByStatusNot(eq(testOrg.getId()), eq(salesRepUser.getId()), eq(TaskStatus.COMPLETED))).thenReturn(4L);
        when(taskRepository.countTasksForSalesRepByStatus(eq(testOrg.getId()), eq(salesRepUser.getId()), eq(TaskStatus.COMPLETED))).thenReturn(2L);
        when(taskRepository.countOverdueTasksForSalesRep(eq(testOrg.getId()), eq(salesRepUser.getId()), eq(TaskStatus.COMPLETED), any())).thenReturn(1L);

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(5L, response.totalLeads());
        assertEquals(5L, response.leadsByStatus().get(LeadStatus.NEW));
        assertEquals(6L, response.totalTasks());
        assertEquals(4L, response.pendingTasks());
        assertEquals(2L, response.completedTasks());
        assertEquals(1L, response.overdueTasks());
    }
}
