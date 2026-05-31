package com.pulsecheck.domain.entity;

import com.pulsecheck.domain.enums.CheckStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitor_checks")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitorCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private String region = "us-east-1";

    @Column(name = "checked_at", updatable = false)
    @Builder.Default
    private Instant checkedAt = Instant.now();
}
