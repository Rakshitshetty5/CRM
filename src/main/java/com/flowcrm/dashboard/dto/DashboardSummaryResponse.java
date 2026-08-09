package com.flowcrm.dashboard.dto;

import com.flowcrm.common.enums.LeadStatus;

import java.util.Map;

public record DashboardSummaryResponse(
        long totalLeads,
        Map<LeadStatus, Long> leadsByStatus,
        long totalTasks,
        long pendingTasks,
        long completedTasks,
        long overdueTasks
) {
}
