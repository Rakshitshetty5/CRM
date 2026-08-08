package com.flowcrm.dashboard.dto;

public record DashboardResponse(
        long totalLeads,
        long newLeads,
        long qualifiedLeads,
        long wonLeads,
        long lostLeads,
        long pendingTasks,
        long overdueTasks
) {
}
