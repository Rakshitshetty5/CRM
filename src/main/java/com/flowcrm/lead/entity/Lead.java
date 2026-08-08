package com.flowcrm.lead.entity;

import com.flowcrm.auth.entity.User;
import com.flowcrm.common.audit.BaseEntity;
import com.flowcrm.common.enums.LeadSource;
import com.flowcrm.common.enums.LeadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "leads",
        indexes = {
                @Index(name = "idx_lead_email", columnList = "email"),
                @Index(name = "idx_lead_status", columnList = "status"),
                @Index(name = "idx_lead_assigned_to", columnList = "assigned_to"),
                @Index(name = "idx_lead_company", columnList = "company")
        }
)
public class Lead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadSource source;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_to", nullable = false)
    private User assignedTo;
}