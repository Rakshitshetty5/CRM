package com.flowcrm.task.service;

import com.flowcrm.auth.entity.User;
import com.flowcrm.auth.repository.UserRepository;
import com.flowcrm.auth.security.UserPrincipal;
import com.flowcrm.common.security.UserContext;
import com.flowcrm.common.enums.ActivityType;
import com.flowcrm.common.enums.LeadSource;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.common.enums.Role;
import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.common.exception.ResourceNotFoundException;
import com.flowcrm.lead.entity.Lead;
import com.flowcrm.lead.entity.LeadActivity;
import com.flowcrm.lead.repository.LeadActivityRepository;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.organization.entity.Organization;
import com.flowcrm.task.dto.CreateTaskRequest;
import com.flowcrm.task.dto.TaskResponse;
import com.flowcrm.task.dto.UpdateTaskRequest;
import com.flowcrm.task.dto.UpdateTaskStatusRequest;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeadActivityRepository leadActivityRepository;

    @Mock
    private UserContext userContext;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User authUser;
    private User assignedUser;
    private Lead testLead;
    private Task testTask;
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

        assignedUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Sales")
                .lastName("Rep")
                .email("sales@flowcrm.com")
                .password("password")
                .role(Role.SALES_REP)
                .active(true)
                .organization(testOrg)
                .build();

        testLead = Lead.builder()
                .id(UUID.randomUUID())
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@abc.com")
                .status(LeadStatus.NEW)
                .source(LeadSource.WEBSITE)
                .assignedTo(authUser)
                .organization(testOrg)
                .build();

        testTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Send pricing details")
                .description("Send enterprise pricing PDF")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDateTime.now().plusDays(2))
                .lead(testLead)
                .assignedTo(assignedUser)
                .organization(testOrg)
                .build();

        lenient().when(userContext.getUserId()).thenReturn(authUser.getId());

        UserPrincipal principal = new UserPrincipal(authUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Create Task - Success")
    void createTask_Success() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Send pricing details", "Send enterprise pricing PDF",
                testLead.getId(), assignedUser.getId(), TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2)
        );

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(testLead.getId(), testOrg.getId())).thenReturn(Optional.of(testLead));
        when(userRepository.findByIdAndOrganizationId(assignedUser.getId(), testOrg.getId())).thenReturn(Optional.of(assignedUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals(testTask.getId(), response.id());
        assertEquals(TaskStatus.PENDING, response.status());
        assertEquals(testLead.getId(), response.leadId());
        assertEquals(assignedUser.getId(), response.assignedTo());

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertEquals(TaskStatus.PENDING, taskCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Create Task - Lead not found throws ResourceNotFoundException")
    void createTask_LeadNotFound() {
        UUID unknownLeadId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
                "Task", null, unknownLeadId, assignedUser.getId(), TaskPriority.LOW,
                LocalDateTime.now().plusDays(1)
        );

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(eq(unknownLeadId), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.createTask(request));
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Task - Assigned user not found throws ResourceNotFoundException")
    void createTask_UserNotFound() {
        UUID unknownUserId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
                "Task", null, testLead.getId(), unknownUserId, TaskPriority.LOW,
                LocalDateTime.now().plusDays(1)
        );

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(leadRepository.findByIdAndOrganizationId(testLead.getId(), testOrg.getId())).thenReturn(Optional.of(testLead));
        when(userRepository.findByIdAndOrganizationId(eq(unknownUserId), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.createTask(request));
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get Tasks - Success with pagination and filters")
    void getTasks_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

        Page<TaskResponse> result = taskService.getTasks(TaskStatus.PENDING, TaskPriority.HIGH, assignedUser.getId(), testLead.getId(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testTask.getId(), result.getContent().get(0).id());
    }

    @Test
    @DisplayName("Get Task By Id - Success")
    void getTaskById_Success() {
        UUID taskId = testTask.getId();
        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(taskRepository.findByIdAndOrganizationId(taskId, testOrg.getId())).thenReturn(Optional.of(testTask));

        TaskResponse response = taskService.getTaskById(taskId);

        assertNotNull(response);
        assertEquals(taskId, response.id());
        assertEquals("Send pricing details", response.title());
    }

    @Test
    @DisplayName("Get Task By Id - Not found throws ResourceNotFoundException")
    void getTaskById_NotFound() {
        UUID taskId = UUID.randomUUID();
        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(taskRepository.findByIdAndOrganizationId(eq(taskId), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(taskId));
    }

    @Test
    @DisplayName("Update Task - Success")
    void updateTask_Success() {
        UUID taskId = testTask.getId();
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated title", "Updated description",
                assignedUser.getId(), TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(3)
        );

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(taskRepository.findByIdAndOrganizationId(taskId, testOrg.getId())).thenReturn(Optional.of(testTask));
        when(userRepository.findByIdAndOrganizationId(assignedUser.getId(), testOrg.getId())).thenReturn(Optional.of(assignedUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.updateTask(taskId, request);

        assertNotNull(response);
        assertEquals(TaskStatus.PENDING, response.status()); // Status must not change
    }

    @Test
    @DisplayName("Update Task Status - Success without COMPLETED")
    void updateTaskStatus_ToInProgress() {
        UUID taskId = testTask.getId();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(taskRepository.findByIdAndOrganizationId(taskId, testOrg.getId())).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        taskService.updateTaskStatus(taskId, request);

        verify(leadActivityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Task Status - COMPLETED to IN_PROGRESS throws IllegalArgumentException")
    void updateTaskStatus_CompletedToInProgress_ThrowsException() {
        UUID taskId = testTask.getId();
        testTask.setStatus(TaskStatus.COMPLETED);
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(taskRepository.findByIdAndOrganizationId(taskId, testOrg.getId())).thenReturn(Optional.of(testTask));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> taskService.updateTaskStatus(taskId, request));
        assertEquals("Invalid status transition from COMPLETED to IN_PROGRESS", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }


    @Test
    @DisplayName("Update Task Status - COMPLETED creates LeadActivity")
    void updateTaskStatus_Completed_CreatesLeadActivity() {
        UUID taskId = testTask.getId();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);

        Task completedTask = Task.builder()
                .id(taskId)
                .title("Send pricing details")
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDateTime.now().plusDays(2))
                .lead(testLead)
                .assignedTo(assignedUser)
                .organization(testOrg)
                .build();

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(taskRepository.findByIdAndOrganizationId(taskId, testOrg.getId())).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(completedTask);

        taskService.updateTaskStatus(taskId, request);

        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(leadActivityRepository).save(activityCaptor.capture());
        LeadActivity savedActivity = activityCaptor.getValue();

        assertEquals(ActivityType.TASK_COMPLETED, savedActivity.getType());
        assertTrue(savedActivity.getDescription().contains("Send pricing details"));
        assertEquals(authUser.getId(), savedActivity.getPerformedBy().getId());
        assertEquals(testLead.getId(), savedActivity.getLead().getId());
    }

    @Test
    @DisplayName("Get Task By Id - Sales Rep Creator can access task assigned to another user")
    void getTaskById_SalesRepCreator_Success() {
        User salesRepCreator = User.builder()
                .id(UUID.randomUUID())
                .firstName("Creator")
                .lastName("Rep")
                .email("creator@flowcrm.com")
                .role(Role.SALES_REP)
                .active(true)
                .organization(testOrg)
                .build();

        Task creatorTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Task created by Sales Rep")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(1))
                .lead(testLead)
                .assignedTo(assignedUser)
                .organization(testOrg)
                .build();
        creatorTask.setCreatedBy(salesRepCreator.getId());

        when(userContext.getUserId()).thenReturn(salesRepCreator.getId());
        when(userRepository.findById(salesRepCreator.getId())).thenReturn(Optional.of(salesRepCreator));
        when(taskRepository.findByIdAndOrganizationId(creatorTask.getId(), testOrg.getId())).thenReturn(Optional.of(creatorTask));

        UserPrincipal principal = new UserPrincipal(salesRepCreator);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        TaskResponse response = taskService.getTaskById(creatorTask.getId());

        assertNotNull(response);
        assertEquals(salesRepCreator.getId(), response.createdBy());
    }

    @Test
    @DisplayName("Get Task By Id - Sales Rep Non-Creator and Non-Assignee is denied access")
    void getTaskById_SalesRepUnrelated_Denied() {
        User salesRepUnrelated = User.builder()
                .id(UUID.randomUUID())
                .firstName("Unrelated")
                .lastName("Rep")
                .email("unrelated@flowcrm.com")
                .role(Role.SALES_REP)
                .active(true)
                .organization(testOrg)
                .build();

        testTask.setCreatedBy(authUser.getId()); // Created by Admin, assigned to assignedUser

        when(userContext.getUserId()).thenReturn(salesRepUnrelated.getId());
        when(userRepository.findById(salesRepUnrelated.getId())).thenReturn(Optional.of(salesRepUnrelated));
        when(taskRepository.findByIdAndOrganizationId(testTask.getId(), testOrg.getId())).thenReturn(Optional.of(testTask));

        UserPrincipal principal = new UserPrincipal(salesRepUnrelated);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> taskService.getTaskById(testTask.getId()));
    }
}
