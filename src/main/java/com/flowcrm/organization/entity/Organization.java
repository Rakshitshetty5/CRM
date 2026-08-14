package com.flowcrm.organization.entity;

import com.flowcrm.common.audit.BaseEntity;
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
        name = "organizations",
        indexes = {
                @Index(name = "idx_organization_slug", columnList = "slug", unique = true),
                @Index(name = "idx_organization_name", columnList = "name", unique = true)
        }
)
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;
}
