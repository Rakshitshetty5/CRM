package com.flowcrm.task.repository;

import com.flowcrm.common.enums.TaskPriority;
import com.flowcrm.common.enums.TaskStatus;
import com.flowcrm.task.entity.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> filterTasks(UUID organizationId, UUID currentUserId, boolean isAdmin, TaskStatus status, TaskPriority priority, UUID assignedToFilter, UUID leadId) {
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
