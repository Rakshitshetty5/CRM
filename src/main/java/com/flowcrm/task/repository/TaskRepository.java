package com.flowcrm.task.repository;

import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    Optional<Task> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, TaskStatus status);
    long countByOrganizationIdAndStatusNot(UUID organizationId, TaskStatus status);
    long countByOrganizationIdAndStatusNotAndDueDateBefore(UUID organizationId, TaskStatus status, LocalDateTime now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.organization.id = :organizationId AND (t.assignedTo.id = :userId OR t.createdBy = :userId)")
    long countTasksForSalesRep(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.organization.id = :organizationId AND (t.assignedTo.id = :userId OR t.createdBy = :userId) AND t.status = :status")
    long countTasksForSalesRepByStatus(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.organization.id = :organizationId AND (t.assignedTo.id = :userId OR t.createdBy = :userId) AND t.status != :status")
    long countTasksForSalesRepByStatusNot(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.organization.id = :organizationId AND (t.assignedTo.id = :userId OR t.createdBy = :userId) AND t.status != :status AND t.dueDate < :now")
    long countOverdueTasksForSalesRep(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId, @Param("status") TaskStatus status, @Param("now") LocalDateTime now);
}

