package com.flowcrm.lead.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.common.enums.ActivityType;
import com.flowcrm.common.enums.LeadSource;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.lead.dto.*;
import com.flowcrm.lead.service.LeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LeadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LeadService leadService;

    @InjectMocks
    private LeadController leadController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(leadController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/leads - Success HTTP 201 Created")
    void createLead_Success() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateLeadRequest request = new CreateLeadRequest(
                "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech", LeadSource.WEBSITE, "Notes"
        );

        LeadResponse response = new LeadResponse(
                leadId, "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech", LeadStatus.NEW, LeadSource.WEBSITE, "Notes", userId, "Sales Rep", userId,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(leadService.createLead(any(CreateLeadRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lead created successfully"))
                .andExpect(jsonPath("$.data.id").value(leadId.toString()))
                .andExpect(jsonPath("$.data.firstName").value("Rahul"))
                .andExpect(jsonPath("$.data.assignedToName").value("Sales Rep"))
                .andExpect(jsonPath("$.data.status").value("NEW"));
    }

    @Test
    @DisplayName("GET /api/v1/leads - Success HTTP 200 with Pagination & Filters")
    void getLeads_Success() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LeadResponse response = new LeadResponse(
                leadId, "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech", LeadStatus.QUALIFIED, LeadSource.WEBSITE, "Notes", userId, "Sales Rep", userId,
                LocalDateTime.now(), LocalDateTime.now()
        );

        PageImpl<LeadResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(leadService.getLeads(eq(LeadStatus.QUALIFIED), eq(userId), eq("rahul"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/leads")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "QUALIFIED")
                        .param("assignedTo", userId.toString())
                        .param("search", "rahul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(leadId.toString()))
                .andExpect(jsonPath("$.content[0].assignedToName").value("Sales Rep"))
                .andExpect(jsonPath("$.content[0].status").value("QUALIFIED"));
    }

    @Test
    @DisplayName("GET /api/v1/leads/{leadId} - Success HTTP 200")
    void getLeadById_Success() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LeadResponse response = new LeadResponse(
                leadId, "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech", LeadStatus.NEW, LeadSource.WEBSITE, "Notes", userId, "Sales Rep", userId,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(leadService.getLeadById(leadId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/leads/{leadId}", leadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leadId.toString()))
                .andExpect(jsonPath("$.firstName").value("Rahul"));
    }

    @Test
    @DisplayName("PUT /api/v1/leads/{leadId} - Success HTTP 200")
    void updateLead_Success() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UpdateLeadRequest request = new UpdateLeadRequest(
                "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech Updated", LeadSource.REFERRAL, "Updated notes"
        );

        LeadResponse response = new LeadResponse(
                leadId, "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech Updated", LeadStatus.NEW, LeadSource.REFERRAL, "Updated notes", userId, "Sales Rep", userId,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(leadService.updateLead(eq(leadId), any(UpdateLeadRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/leads/{leadId}", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lead updated successfully"))
                .andExpect(jsonPath("$.data.company").value("ABC Tech Updated"))
                .andExpect(jsonPath("$.data.source").value("REFERRAL"));
    }

    @Test
    @DisplayName("PATCH /api/v1/leads/{leadId}/status - Success HTTP 200")
    void updateLeadStatus_Success() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UpdateLeadStatusRequest request = new UpdateLeadStatusRequest(LeadStatus.QUALIFIED);

        LeadResponse response = new LeadResponse(
                leadId, "Rahul", "Sharma", "rahul@abc.com", "+919876543210",
                "ABC Tech", LeadStatus.QUALIFIED, LeadSource.WEBSITE, "Notes", userId, "Sales Rep", userId,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(leadService.updateLeadStatus(eq(leadId), any(UpdateLeadStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/leads/{leadId}/status", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lead status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("QUALIFIED"));
    }

    @Test
    @DisplayName("GET /api/v1/leads/{leadId}/activities - Success HTTP 200")
    void getLeadActivities_Success() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LeadActivityResponse response = new LeadActivityResponse(
                UUID.randomUUID(), ActivityType.LEAD_CREATED, "Lead created", userId, LocalDateTime.now()
        );

        when(leadService.getLeadActivities(leadId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/leads/{leadId}/activities", leadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("LEAD_CREATED"))
                .andExpect(jsonPath("$[0].description").value("Lead created"));
    }
}
