package com.flowcrm.kafka.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.kafka.dto.EventEnvelope;
import com.flowcrm.kafka.entity.ProcessedEvent;
import com.flowcrm.kafka.repository.ProcessedEventRepository;
import com.flowcrm.notification.service.NotificationMetadataEnricher;
import com.flowcrm.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationMetadataEnricher notificationMetadataEnricher;
    private final MeterRegistry meterRegistry;

    @Transactional
    public boolean processEvent(EventEnvelope envelope) {
        UUID eventId = extractEventId(envelope);
        if (eventId == null) {
            log.warn("Received invalid or null event envelope/ID");
            meterRegistry.counter("kafka.events.failed", "reason", "invalid_envelope").increment();
            return false;
        }

        // 1. Idempotency Check
        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event skipped: eventId={}", eventId);
            return false;
        }

        // 2. Perform event-specific processing (e.g. Notification creation for LeadAssigned, TaskFollowUpDue)
        try {
            if ("LeadAssigned".equalsIgnoreCase(envelope.eventType())) {
                processLeadAssignedEvent(envelope);
            } else if ("TaskFollowUpDue".equalsIgnoreCase(envelope.eventType())) {
                processTaskFollowUpDueEvent(envelope);
            }
        } catch (Exception e) {
            meterRegistry.counter("kafka.events.failed", "eventType", envelope.eventType() != null ? envelope.eventType() : "unknown").increment();
            throw e;
        }

        // 3. Persist processed event ID after side effects succeed
        try {
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(LocalDateTime.now())
                    .build();
            processedEventRepository.saveAndFlush(processedEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event skipped (concurrent processing): eventId={}", eventId);
            return false;
        }

        // 4. Extract organization context & log successful processing
        UUID organizationId = extractOrganizationId(envelope.payload());

        log.info("Successfully processed event: eventId={}, eventType={}, aggregateType={}, aggregateId={}, organizationId={}",
                eventId, envelope.eventType(), envelope.aggregateType(), envelope.aggregateId(), organizationId);
        meterRegistry.counter("kafka.events.processed", "eventType", envelope.eventType() != null ? envelope.eventType() : "unknown").increment();

        return true;
    }


    private void processLeadAssignedEvent(EventEnvelope envelope) {
        if (envelope.payload() == null || envelope.payload().isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(envelope.payload());
            UUID organizationId = root.has("organizationId") && !root.get("organizationId").isNull()
                    ? UUID.fromString(root.get("organizationId").asText()) : null;
            UUID leadId = root.has("leadId") && !root.get("leadId").isNull()
                    ? UUID.fromString(root.get("leadId").asText()) : envelope.aggregateId();
            UUID assignedTo = root.has("assignedTo") && !root.get("assignedTo").isNull()
                    ? UUID.fromString(root.get("assignedTo").asText()) : null;

            if (assignedTo != null && organizationId != null) {
                Map<String, Object> metadata = notificationMetadataEnricher.buildLeadAssignedMetadata(leadId, root);
                notificationService.createNotification(
                        assignedTo,
                        organizationId,
                        "LEAD_ASSIGNED",
                        "New Lead Assigned",
                        "A new lead has been assigned to you.",
                        leadId,
                        metadata
                );
            }
        } catch (Exception e) {
            log.error("Failed to process LeadAssigned event for notification: eventId={}", envelope.id(), e);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Failed to process LeadAssigned notification", e);
        }
    }

    private void processTaskFollowUpDueEvent(EventEnvelope envelope) {
        if (envelope.payload() == null || envelope.payload().isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(envelope.payload());
            UUID organizationId = root.has("organizationId") && !root.get("organizationId").isNull()
                    ? UUID.fromString(root.get("organizationId").asText()) : null;
            UUID taskId = root.has("taskId") && !root.get("taskId").isNull()
                    ? UUID.fromString(root.get("taskId").asText())
                    : (root.has("entityId") && !root.get("entityId").isNull()
                        ? UUID.fromString(root.get("entityId").asText())
                        : envelope.aggregateId());
            UUID assignedTo = root.has("assignedTo") && !root.get("assignedTo").isNull()
                    ? UUID.fromString(root.get("assignedTo").asText()) : null;

            if (assignedTo != null && organizationId != null) {
                Map<String, Object> metadata = notificationMetadataEnricher.buildTaskFollowUpDueMetadata(taskId, root);
                notificationService.createNotification(
                        assignedTo,
                        organizationId,
                        "TASK_FOLLOW_UP_DUE",
                        "Task Follow-up Due",
                        "Your follow-up task is due.",
                        taskId,
                        metadata
                );
            }
        } catch (Exception e) {
            log.error("Failed to process TaskFollowUpDue event for notification: eventId={}", envelope.id(), e);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Failed to process TaskFollowUpDue notification", e);
        }
    }

    private UUID extractEventId(EventEnvelope envelope) {
        if (envelope == null) {
            return null;
        }
        if (envelope.id() != null) {
            return envelope.id();
        }
        if (envelope.payload() != null && !envelope.payload().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(envelope.payload());
                if (root.has("eventId") && !root.get("eventId").isNull()) {
                    return UUID.fromString(root.get("eventId").asText());
                }
            } catch (Exception e) {
                log.warn("Could not extract eventId from payload JSON", e);
            }
        }
        return null;
    }

    private UUID extractOrganizationId(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root.has("organizationId") && !root.get("organizationId").isNull()) {
                return UUID.fromString(root.get("organizationId").asText());
            }
        } catch (Exception e) {
            log.warn("Could not extract organizationId from event payload", e);
        }
        return null;
    }
}

