package com.flowcrm.kafka.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcrm.kafka.dto.EventEnvelope;
import com.flowcrm.kafka.entity.ProcessedEvent;
import com.flowcrm.kafka.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public boolean processEvent(EventEnvelope envelope) {
        if (envelope == null || envelope.id() == null) {
            log.warn("Received invalid or null event envelope");
            return false;
        }

        UUID eventId = envelope.id();

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event skipped: eventId={}", eventId);
            return false;
        }

        UUID organizationId = extractOrganizationId(envelope.payload());

        log.info("Successfully processed event: eventId={}, eventType={}, aggregateType={}, aggregateId={}, organizationId={}",
                eventId, envelope.eventType(), envelope.aggregateType(), envelope.aggregateId(), organizationId);

        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(LocalDateTime.now())
                .build();
        processedEventRepository.save(processedEvent);

        return true;
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
