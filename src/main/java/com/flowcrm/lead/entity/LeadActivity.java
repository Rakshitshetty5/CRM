package com.flowcrm.lead.entity;

import com.flowcrm.auth.entity.User;
import com.flowcrm.common.audit.BaseEntity;
import com.flowcrm.common.enums.ActivityType;
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
        name = "lead_activities",
        indexes = {
                @Index(name = "idx_activity_lead", columnList = "lead_id"),
                @Index(name = "idx_activity_created_at", columnList = "created_at")
        }
)
public class LeadActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;
}