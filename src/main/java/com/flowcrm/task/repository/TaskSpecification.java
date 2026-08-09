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

    public static Specification<Task> filterTasks(TaskStatus status, TaskPriority priority, UUID assignedTo, UUID leadId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (assignedTo != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedTo));
            }

            if (leadId != null) {
                predicates.add(cb.equal(root.get("lead").get("id"), leadId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
