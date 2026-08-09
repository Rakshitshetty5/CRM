package com.flowcrm.organization.repository;

import com.flowcrm.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
}
