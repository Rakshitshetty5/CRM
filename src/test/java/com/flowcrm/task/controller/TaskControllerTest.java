package com.flowcrm.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.common.exception.GlobalExceptionHandler;
import com.flowcrm.task.dto.CreateTaskRequest;
import com.flowcrm.task.dto.TaskResponse;
import com.flowcrm.task.dto.UpdateTaskRequest;
import com.flowcrm.task.dto.UpdateTaskStatusRequest;
import com.flowcrm.task.service.TaskService;
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
class TaskControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Success HTTP 201")
    void createTask_Success() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateTaskRequest request = new CreateTaskRequest(
                "Send pricing details", "Send enterprise pricing PDF",
                leadId, userId, TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2)
        );

        TaskResponse response = new TaskResponse(
                taskId, "Send pricing details", "Send enterprise pricing PDF",
                TaskStatus.PENDING, TaskPriority.HIGH, LocalDateTime.now().plusDays(2),
                leadId, "Acme Lead", userId, "Sales Rep", userId, LocalDateTime.now(), LocalDateTime.now()
        );

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task created successfully"))
                .andExpect(jsonPath("$.data.id").value(taskId.toString()))
                .andExpect(jsonPath("$.data.leadName").value("Acme Lead"))
                .andExpect(jsonPath("$.data.assignedToName").value("Sales Rep"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - Success HTTP 200 paginated")
    void getTasks_Success() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TaskResponse response = new TaskResponse(
                taskId, "Send pricing details", null,
                TaskStatus.PENDING, TaskPriority.HIGH, LocalDateTime.now().plusDays(2),
                leadId, "Acme Lead", userId, "Sales Rep", userId, LocalDateTime.now(), LocalDateTime.now()
        );

        PageImpl<TaskResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(taskService.getTasks(eq(TaskStatus.PENDING), any(), eq(userId), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "PENDING")
                        .param("assignedTo", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.content[0].leadName").value("Acme Lead"))
                .andExpect(jsonPath("$.content[0].assignedToName").value("Sales Rep"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{taskId} - Success HTTP 200")
    void getTaskById_Success() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TaskResponse response = new TaskResponse(
                taskId, "Send pricing details", null,
                TaskStatus.PENDING, TaskPriority.HIGH, LocalDateTime.now().plusDays(2),
                leadId, "Acme Lead", userId, "Sales Rep", userId, LocalDateTime.now(), LocalDateTime.now()
        );

        when(taskService.getTaskById(taskId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("Send pricing details"));
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{taskId} - Success HTTP 200")
    void updateTask_Success() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated title", "Updated description",
                userId, TaskPriority.MEDIUM, LocalDateTime.now().plusDays(3)
        );

        TaskResponse response = new TaskResponse(
                taskId, "Updated title", "Updated description",
                TaskStatus.PENDING, TaskPriority.MEDIUM, LocalDateTime.now().plusDays(3),
                leadId, "Acme Lead", userId, "Sales Rep", userId, LocalDateTime.now(), LocalDateTime.now()
        );

        when(taskService.updateTask(eq(taskId), any(UpdateTaskRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated title"))
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{taskId}/status - Success HTTP 200")
    void updateTaskStatus_Success() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);

        TaskResponse response = new TaskResponse(
                taskId, "Send pricing details", null,
                TaskStatus.COMPLETED, TaskPriority.HIGH, LocalDateTime.now().plusDays(2),
                leadId, "Acme Lead", userId, "Sales Rep", userId, LocalDateTime.now(), LocalDateTime.now()
        );

        when(taskService.updateTaskStatus(eq(taskId), any(UpdateTaskStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

}
