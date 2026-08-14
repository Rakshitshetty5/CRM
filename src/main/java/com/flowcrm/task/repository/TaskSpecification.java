package com.flowcrm.task.repository;

import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.task.entity.Task;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> filterTasks(UUID organizationId, UUID currentUserId, boolean isAdmin, TaskStatus status, TaskPriority priority, UUID assignedToFilter, UUID leadId) {
        return filterTasks(organizationId, currentUserId, isAdmin, status, priority, assignedToFilter, leadId, false);
    }

    public static Specification<Task> filterTasks(UUID organizationId, UUID currentUserId, boolean isAdmin, TaskStatus status, TaskPriority priority, UUID assignedToFilter, UUID leadId, boolean isUnsorted) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }

            if (!isAdmin && currentUserId != null) {
                Predicate isAssignee = cb.equal(root.get("assignedTo").get("id"), currentUserId);
                Predicate isCreator = cb.equal(root.get("createdBy"), currentUserId);
                predicates.add(cb.or(isAssignee, isCreator));
            }

            if (assignedToFilter != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToFilter));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (leadId != null) {
                predicates.add(cb.equal(root.get("lead").get("id"), leadId));
            }

            if (isUnsorted && query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                Expression<Integer> isCompleted = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("status"), TaskStatus.COMPLETED), 1)
                        .otherwise(0);

                Expression<LocalDateTime> incompleteDueDate = cb.<LocalDateTime>selectCase()
                        .when(cb.equal(root.get("status"), TaskStatus.COMPLETED), cb.nullLiteral(LocalDateTime.class))
                        .otherwise(root.get("dueDate"));

                Expression<LocalDateTime> completedUpdatedAt = cb.<LocalDateTime>selectCase()
                        .when(cb.equal(root.get("status"), TaskStatus.COMPLETED), root.get("updatedAt"))
                        .otherwise(cb.nullLiteral(LocalDateTime.class));

                query.orderBy(
                        cb.asc(isCompleted),
                        cb.asc(incompleteDueDate),
                        cb.desc(completedUpdatedAt)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
