package com.flowcrm.kafka.consumer;

import com.flowcrm.kafka.dto.EventEnvelope;
import com.flowcrm.kafka.service.EventProcessingService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeadEventConsumer {

    private final EventProcessingService eventProcessingService;
    private final MeterRegistry meterRegistry;

    @RetryableTopic(
            attempts = "${kafka.consumer.retry.max-attempts:3}",
            backOff = @BackOff(delay = 1000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "${app.kafka.topics.lead:leads.events}", groupId = "${spring.kafka.consumer.group-id:flowcrm-monolith}")
    public void consumeLeadEvent(EventEnvelope envelope) {
        log.info("Received event from topic leads.events: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
                envelope != null ? envelope.id() : null,
                envelope != null ? envelope.eventType() : null,
                envelope != null ? envelope.aggregateType() : null,
                envelope != null ? envelope.aggregateId() : null);

        eventProcessingService.processEvent(envelope);
    }

    @DltHandler
    public void handleDlt(EventEnvelope envelope) {
        log.error("Event sent to DLT (leads.events.DLT): eventId={}, eventType={}, aggregateType={}, aggregateId={}",
                envelope != null ? envelope.id() : null,
                envelope != null ? envelope.eventType() : null,
                envelope != null ? envelope.aggregateType() : null,
                envelope != null ? envelope.aggregateId() : null);
        meterRegistry.counter("kafka.events.dlt", "topic", "leads.events.DLT").increment();
    }
}

