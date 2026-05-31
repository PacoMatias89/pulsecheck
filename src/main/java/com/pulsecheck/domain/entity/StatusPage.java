package com.pulsecheck.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "status_pages")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusPage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "custom_domain")
    private String customDomain;

    @Column
    private String description;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean publicPage = true;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", nullable = false)
    @Builder.Default
    private String primaryColor = "#6366f1";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
