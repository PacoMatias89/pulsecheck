package com.pulsecheck.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMonitorToPageRequest(
        @NotNull UUID monitorId,
        String displayName,
        Integer orderIndex
) {}
