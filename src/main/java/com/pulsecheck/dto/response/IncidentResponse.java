package com.pulsecheck.dto.response;

import com.pulsecheck.domain.entity.Incident;
import com.pulsecheck.domain.enums.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID monitorId,
        String monitorName,
        String title,
        IncidentStatus status,
        Instant startedAt,
        Instant resolvedAt,
        Long durationSeconds
) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(
                i.getId(),
                i.getMonitor().getId(),
                i.getMonitor().getName(),
                i.getTitle(),
                i.getStatus(),
                i.getStartedAt(),
                i.getResolvedAt(),
                i.getDurationSeconds()
        );
    }
}
