package com.flowcrm.lead.repository;

import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.lead.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    @Override
    @EntityGraph(attributePaths = {"assignedTo"})
    Page<Lead> findAll(Specification<Lead> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"assignedTo"})
    Optional<Lead> findByIdAndOrganizationId(UUID id, UUID organizationId);


    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndAssignedToId(UUID organizationId, UUID assignedToId);

    @Query("SELECT l.status, COUNT(l) FROM Lead l WHERE l.organization.id = :organizationId GROUP BY l.status")
    List<Object[]> countLeadsByStatusForAdmin(@Param("organizationId") UUID organizationId);

    @Query("SELECT l.status, COUNT(l) FROM Lead l WHERE l.organization.id = :organizationId AND l.assignedTo.id = :assignedToId GROUP BY l.status")
    List<Object[]> countLeadsByStatusForSalesRep(@Param("organizationId") UUID organizationId, @Param("assignedToId") UUID assignedToId);
}


