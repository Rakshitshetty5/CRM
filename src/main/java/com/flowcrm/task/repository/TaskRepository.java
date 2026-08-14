package com.flowcrm.task.repository;

import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    @Override
    @EntityGraph(attributePaths = {"lead", "assignedTo"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

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

    @EntityGraph(attributePaths = {"organization", "assignedTo"})
    @Query("SELECT t FROM Task t WHERE t.reminderSent = false AND t.dueDate <= :now AND t.status != com.flowcrm.common.enums.TaskStatus.COMPLETED AND t.assignedTo IS NOT NULL")
    List<Task> findTasksDueForReminder(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.reminderSent = true WHERE t.id = :taskId AND t.reminderSent = false")
    int markReminderSent(@Param("taskId") UUID taskId);
}


