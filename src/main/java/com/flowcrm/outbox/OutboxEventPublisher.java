package com.flowcrm.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flowcrm.common.enums.OutboxStatus;
import com.flowcrm.common.event.DomainEvent;
import com.flowcrm.common.event.LeadAssignedEvent;
import com.flowcrm.common.event.LeadCreatedEvent;
import com.flowcrm.common.event.LeadStatusChangedEvent;
import com.flowcrm.common.event.LeadUpdatedEvent;
import com.flowcrm.common.event.TaskAssignedEvent;
import com.flowcrm.common.event.TaskCreatedEvent;
import com.flowcrm.common.event.TaskFollowUpDue;
import com.flowcrm.common.event.TaskStatusChangedEvent;
import com.flowcrm.outbox.entity.OutboxEvent;
import com.flowcrm.outbox.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                @Autowired(required = false) ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository) {
        this(outboxEventRepository, null);
    }

    @Transactional
    public OutboxEvent publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String aggregateType = determineAggregateType(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(event.entityId())
                    .eventType(event.eventType())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            return outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish domain event to outbox", e);
        }
    }

    private String determineAggregateType(DomainEvent event) {
        if (event instanceof LeadCreatedEvent || event instanceof LeadUpdatedEvent
                || event instanceof LeadAssignedEvent || event instanceof LeadStatusChangedEvent) {
            return "LEAD";
        } else if (event instanceof TaskCreatedEvent || event instanceof TaskAssignedEvent
                || event instanceof TaskStatusChangedEvent || event instanceof TaskFollowUpDue) {
            return "TASK";
        }
        return "UNKNOWN";
    }
}
