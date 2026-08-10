package com.flowcrm.outbox.repository;

import com.flowcrm.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flowcrm.common.enums.OutboxStatus;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
