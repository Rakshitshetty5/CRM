package com.flowcrm.outbox;

import com.flowcrm.common.enums.OutboxStatus;
import com.flowcrm.outbox.entity.OutboxEvent;
import com.flowcrm.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxResultHandler {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${outbox.retry.max-attempts:3}")
    private int maxAttempts;

    @Transactional
    public void handleEventPublished(OutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional
    public void handleEventFailed(OutboxEvent event, String errorMessage) {
        int attempts = event.getRetryCount() + 1;
        event.setRetryCount(attempts);
        event.setErrorMessage(errorMessage);

        if (attempts >= maxAttempts) {
            event.setStatus(OutboxStatus.FAILED);
        } else {
            event.setStatus(OutboxStatus.PENDING);
        }

        outboxEventRepository.save(event);
    }
}
