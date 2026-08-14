package com.flowcrm.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowcrm.lead.entity.Lead;
import com.flowcrm.lead.repository.LeadRepository;
import com.flowcrm.task.entity.Task;
import com.flowcrm.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMetadataEnricher {

    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;

    public Map<String, Object> buildLeadAssignedMetadata(UUID leadId, JsonNode payloadRoot) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (leadId != null) {
            metadata.put("leadId", leadId.toString());
        }

        String leadName = getJsonString(payloadRoot, "leadName");
        String leadDescription = getJsonString(payloadRoot, "leadDescription");
        String stage = getJsonString(payloadRoot, "stage");

        if ((leadName == null || leadDescription == null || stage == null) && leadId != null) {
            Optional<Lead> leadOpt = leadRepository.findById(leadId);
            if (leadOpt.isPresent()) {
                Lead lead = leadOpt.get();
                if (leadName == null) {
                    leadName = extractLeadName(lead);
                }
                if (leadDescription == null) {
                    leadDescription = lead.getNotes();
                }
                if (stage == null && lead.getStatus() != null) {
                    stage = lead.getStatus().name();
                }
            }
        }

        metadata.put("leadName", leadName);
        metadata.put("leadDescription", leadDescription);
        metadata.put("stage", stage);

        return metadata;
    }

    public Map<String, Object> buildTaskFollowUpDueMetadata(UUID taskId, JsonNode payloadRoot) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (taskId != null) {
            metadata.put("taskId", taskId.toString());
        }

        String taskTitle = getJsonString(payloadRoot, "taskTitle");
        if (taskTitle == null) {
            taskTitle = getJsonString(payloadRoot, "title");
        }
        String taskDescription = getJsonString(payloadRoot, "taskDescription");
        if (taskDescription == null) {
            taskDescription = getJsonString(payloadRoot, "description");
        }
        String leadIdStr = getJsonString(payloadRoot, "leadId");
        String leadName = getJsonString(payloadRoot, "leadName");

        if ((taskTitle == null || taskDescription == null || leadIdStr == null || leadName == null) && taskId != null) {
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                if (taskTitle == null) {
                    taskTitle = task.getTitle();
                }
                if (taskDescription == null) {
                    taskDescription = task.getDescription();
                }
                if (task.getLead() != null) {
                    if (leadIdStr == null && task.getLead().getId() != null) {
                        leadIdStr = task.getLead().getId().toString();
                    }
                    if (leadName == null) {
                        leadName = extractLeadName(task.getLead());
                    }
                }
            }
        }

        metadata.put("taskTitle", taskTitle);
        metadata.put("taskDescription", taskDescription);
        metadata.put("leadId", leadIdStr);
        metadata.put("leadName", leadName);

        return metadata;
    }

    private String extractLeadName(Lead lead) {
        if (lead.getCompany() != null && !lead.getCompany().isBlank()) {
            return lead.getCompany();
        }
        String firstName = lead.getFirstName() != null ? lead.getFirstName() : "";
        String lastName = lead.getLastName() != null ? lead.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return !fullName.isBlank() ? fullName : null;
    }

    private String getJsonString(JsonNode root, String fieldName) {
        if (root != null && root.has(fieldName) && !root.get(fieldName).isNull()) {
            return root.get(fieldName).asText();
        }
        return null;
    }
}
