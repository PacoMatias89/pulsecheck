package com.pulsecheck.dto.request;

import com.pulsecheck.domain.enums.MonitorType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateMonitorRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String url,
        MonitorType type,
        String method,
        Integer expectedStatusCode,
        String keywordContains,
        Map<String, String> requestHeaders,
        @Min(30) @Max(86400) Integer intervalSeconds,
        @Min(1000) @Max(60000) Integer timeoutMs
) {}
