package com.flowcrm.lead.repository;

import com.flowcrm.lead.entity.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {
}
