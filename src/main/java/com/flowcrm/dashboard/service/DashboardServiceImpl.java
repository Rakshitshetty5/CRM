package com.flowcrm.dashboard.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.dashboard.dto.DashboardSummaryResponse;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final UserContext userContext;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        User currentUser = getAuthenticatedUser();
        UUID orgId = currentUser.getOrganization().getId();
        UUID userId = currentUser.getId();
        LocalDateTime now = LocalDateTime.now();

        long totalLeads;
        Map<LeadStatus, Long> leadsByStatus = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            leadsByStatus.put(status, 0L);
        }

        long totalTasks;
        long pendingTasks;
        long completedTasks;
        long overdueTasks;

        if (currentUser.getRole() == Role.ADMIN) {
            totalLeads = leadRepository.countByOrganizationId(orgId);
            List<Object[]> statusCounts = leadRepository.countLeadsByStatusForAdmin(orgId);
            for (Object[] row : statusCounts) {
                LeadStatus status = (LeadStatus) row[0];
                Long count = (Long) row[1];
                leadsByStatus.put(status, count);
            }

            totalTasks = taskRepository.countByOrganizationId(orgId);
            pendingTasks = taskRepository.countByOrganizationIdAndStatusNot(orgId, TaskStatus.COMPLETED);
            completedTasks = taskRepository.countByOrganizationIdAndStatus(orgId, TaskStatus.COMPLETED);
            overdueTasks = taskRepository.countByOrganizationIdAndStatusNotAndDueDateBefore(orgId, TaskStatus.COMPLETED, now);
        } else {
            totalLeads = leadRepository.countByOrganizationIdAndAssignedToId(orgId, userId);
            List<Object[]> statusCounts = leadRepository.countLeadsByStatusForSalesRep(orgId, userId);
            for (Object[] row : statusCounts) {
                LeadStatus status = (LeadStatus) row[0];
                Long count = (Long) row[1];
                leadsByStatus.put(status, count);
            }

            totalTasks = taskRepository.countTasksForSalesRep(orgId, userId);
            pendingTasks = taskRepository.countTasksForSalesRepByStatusNot(orgId, userId, TaskStatus.COMPLETED);
            completedTasks = taskRepository.countTasksForSalesRepByStatus(orgId, userId, TaskStatus.COMPLETED);
            overdueTasks = taskRepository.countOverdueTasksForSalesRep(orgId, userId, TaskStatus.COMPLETED, now);
        }

        return new DashboardSummaryResponse(
                totalLeads,
                leadsByStatus,
                totalTasks,
                pendingTasks,
                completedTasks,
                overdueTasks
        );
    }

    private User getAuthenticatedUser() {
        UUID userId = userContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
