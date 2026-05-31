package com.pulsecheck.domain.entity;

import com.pulsecheck.domain.enums.MonitorStatus;
import com.pulsecheck.domain.enums.MonitorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "monitors")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MonitorType type = MonitorType.HTTPS;

    @Column(nullable = false)
    @Builder.Default
    private String method = "GET";

    @Column(name = "expected_status_code", nullable = false)
    @Builder.Default
    private int expectedStatusCode = 200;

    @Column(name = "keyword_contains")
    private String keywordContains;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> requestHeaders = Map.of();

    @Column(name = "interval_seconds", nullable = false)
    @Builder.Default
    private int intervalSeconds = 300;

    @Column(name = "timeout_ms", nullable = false)
    @Builder.Default
    private int timeoutMs = 30000;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MonitorStatus status = MonitorStatus.PENDING;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private int consecutiveFailures = 0;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_status_change_at")
    private Instant lastStatusChangeAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
