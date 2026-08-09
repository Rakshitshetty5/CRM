package com.flowcrm.lead.repository;

import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.lead.entity.Lead;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LeadSpecification {

    private LeadSpecification() {
        // Private constructor for utility class
    }

    public static Specification<Lead> filterLeads(LeadStatus status, UUID assignedTo, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (assignedTo != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedTo));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), searchPattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), searchPattern);
                Predicate companyMatch = cb.like(cb.lower(root.get("company")), searchPattern);

                predicates.add(cb.or(firstNameMatch, lastNameMatch, emailMatch, companyMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
