package com.flowcrm.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.lead.entity.Lead;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationMetadataEnricherTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private TaskRepository taskRepository;

    private ObjectMapper objectMapper;
    private NotificationMetadataEnricher enricher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        enricher = new NotificationMetadataEnricher(leadRepository, taskRepository);
    }

    @Test
    void testBuildLeadAssignedMetadataFromRepository() {
        UUID leadId = UUID.randomUUID();
        Lead lead = Lead.builder()
                .id(leadId)
                .company("Acme Corp")
                .firstName("John")
                .lastName("Doe")
                .notes("Enterprise customer interested in CRM.")
                .status(LeadStatus.NEW)
                .build();

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        Map<String, Object> metadata = enricher.buildLeadAssignedMetadata(leadId, objectMapper.createObjectNode());

        assertNotNull(metadata);
        assertEquals(leadId.toString(), metadata.get("leadId"));
        assertEquals("Acme Corp", metadata.get("leadName"));
        assertEquals("Enterprise customer interested in CRM.", metadata.get("leadDescription"));
        assertEquals("NEW", metadata.get("stage"));
    }

    @Test
    void testBuildLeadAssignedMetadataFromPayloadOverridingDb() throws Exception {
        UUID leadId = UUID.randomUUID();
        String jsonPayload = String.format(
                "{\"leadId\":\"%s\",\"leadName\":\"Custom Corp\",\"leadDescription\":\"Custom notes\",\"stage\":\"QUALIFIED\"}",
                leadId
        );
        var jsonNode = objectMapper.readTree(jsonPayload);

        Map<String, Object> metadata = enricher.buildLeadAssignedMetadata(leadId, jsonNode);

        assertNotNull(metadata);
        assertEquals(leadId.toString(), metadata.get("leadId"));
        assertEquals("Custom Corp", metadata.get("leadName"));
        assertEquals("Custom notes", metadata.get("leadDescription"));
        assertEquals("QUALIFIED", metadata.get("stage"));

        verifyNoInteractions(leadRepository);
    }

    @Test
    void testBuildLeadAssignedMetadataFullNameFallback() {
        UUID leadId = UUID.randomUUID();
        Lead lead = Lead.builder()
                .id(leadId)
                .company(null)
                .firstName("Alice")
                .lastName("Smith")
                .notes("Personal lead")
                .status(LeadStatus.CONTACTED)
                .build();

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        Map<String, Object> metadata = enricher.buildLeadAssignedMetadata(leadId, objectMapper.createObjectNode());

        assertEquals("Alice Smith", metadata.get("leadName"));
        assertEquals("CONTACTED", metadata.get("stage"));
    }

    @Test
    void testBuildTaskFollowUpDueMetadataFromRepository() {
        UUID taskId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        Lead lead = Lead.builder()
                .id(leadId)
                .company("Acme Corp")
                .firstName("John")
                .lastName("Doe")
                .build();

        Task task = Task.builder()
                .id(taskId)
                .title("Follow up with Acme Corp")
                .description("Contact the customer regarding pricing.")
                .lead(lead)
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Map<String, Object> metadata = enricher.buildTaskFollowUpDueMetadata(taskId, objectMapper.createObjectNode());

        assertNotNull(metadata);
        assertEquals(taskId.toString(), metadata.get("taskId"));
        assertEquals("Follow up with Acme Corp", metadata.get("taskTitle"));
        assertEquals("Contact the customer regarding pricing.", metadata.get("taskDescription"));
        assertEquals(leadId.toString(), metadata.get("leadId"));
        assertEquals("Acme Corp", metadata.get("leadName"));
    }

    @Test
    void testBuildTaskFollowUpDueMetadataFromPayload() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        String jsonPayload = String.format(
                "{\"taskId\":\"%s\",\"taskTitle\":\"Follow up\",\"taskDescription\":\"Call back\",\"leadId\":\"%s\",\"leadName\":\"Acme Corp\"}",
                taskId, leadId
        );
        var jsonNode = objectMapper.readTree(jsonPayload);

        Map<String, Object> metadata = enricher.buildTaskFollowUpDueMetadata(taskId, jsonNode);

        assertNotNull(metadata);
        assertEquals(taskId.toString(), metadata.get("taskId"));
        assertEquals("Follow up", metadata.get("taskTitle"));
        assertEquals("Call back", metadata.get("taskDescription"));
        assertEquals(leadId.toString(), metadata.get("leadId"));
        assertEquals("Acme Corp", metadata.get("leadName"));

        verifyNoInteractions(taskRepository);
    }
}
