package com.pulsecheck.dto.response;

import com.pulsecheck.domain.entity.Monitor;
import com.pulsecheck.domain.enums.MonitorStatus;
import com.pulsecheck.domain.enums.MonitorType;

import java.time.Instant;
import java.util.UUID;

public record MonitorResponse(
        UUID id,
        String name,
        String url,
        MonitorType type,
        String method,
        int expectedStatusCode,
        int intervalSeconds,
        int timeoutMs,
        MonitorStatus status,
        boolean active,
        Instant lastCheckedAt,
        Instant createdAt
) {
    public static MonitorResponse from(Monitor m) {
        return new MonitorResponse(
                m.getId(), m.getName(), m.getUrl(), m.getType(), m.getMethod(),
                m.getExpectedStatusCode(), m.getIntervalSeconds(), m.getTimeoutMs(),
                m.getStatus(), m.isActive(), m.getLastCheckedAt(), m.getCreatedAt()
        );
    }
}
