package com.flowcrm.outbox;

import com.flowcrm.common.enums.OutboxStatus;
import com.flowcrm.outbox.entity.OutboxEvent;
import com.flowcrm.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxResultHandler outboxResultHandler;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.lead:leads.events}")
    private String leadTopic;

    @Value("${app.kafka.topics.task:tasks.events}")
    private String taskTopic;

    @Scheduled(fixedDelayString = "${outbox.polling.fixed-delay:5000}")
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = resolveTopic(event);
                Map<String, Object> messageEnvelope = createEnvelope(event);

                kafkaTemplate.send(topic, event.getAggregateId().toString(), messageEnvelope).get();
                outboxResultHandler.handleEventPublished(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event id: {}", event.getId(), e);
                outboxResultHandler.handleEventFailed(event, e.getMessage());
            }
        }
    }

    private String resolveTopic(OutboxEvent event) {
        String aggregateType = event.getAggregateType();
        if (aggregateType != null && aggregateType.equalsIgnoreCase("LEAD")) {
            return leadTopic;
        } else if (aggregateType != null && aggregateType.equalsIgnoreCase("TASK")) {
            return taskTopic;
        }
        return leadTopic;
    }

    private Map<String, Object> createEnvelope(OutboxEvent event) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("id", event.getId());
        envelope.put("eventType", event.getEventType());
        envelope.put("aggregateType", event.getAggregateType());
        envelope.put("aggregateId", event.getAggregateId());
        envelope.put("payload", event.getPayload());
        return envelope;
    }
}
