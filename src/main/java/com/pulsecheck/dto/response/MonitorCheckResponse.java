package com.pulsecheck.dto.response;

import com.pulsecheck.domain.entity.MonitorCheck;
import com.pulsecheck.domain.enums.CheckStatus;

import java.time.Instant;
import java.util.UUID;

public record MonitorCheckResponse(
        UUID id,
        CheckStatus status,
        Long responseTimeMs,
        Integer statusCode,
        String errorMessage,
        String region,
        Instant checkedAt
) {
    public static MonitorCheckResponse from(MonitorCheck c) {
        return new MonitorCheckResponse(c.getId(), c.getStatus(), c.getResponseTimeMs(),
                c.getStatusCode(), c.getErrorMessage(), c.getRegion(), c.getCheckedAt());
    }
}
