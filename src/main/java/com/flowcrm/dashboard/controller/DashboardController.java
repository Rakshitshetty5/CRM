package com.flowcrm.dashboard.controller;

import com.flowcrm.dashboard.dto.DashboardSummaryResponse;
import com.flowcrm.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "CRM metrics and summary dashboard APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard summary", description = "Retrieves aggregated leads and tasks summary metrics (cached per organization and role)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(response);
    }
}

