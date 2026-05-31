package com.pulsecheck.dto.request;

import com.pulsecheck.domain.enums.MonitorType;
import jakarta.validation.constraints.*;

import java.util.Map;

public record CreateMonitorRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2000) String url,
        MonitorType type,
        String method,
        Integer expectedStatusCode,
        String keywordContains,
        Map<String, String> requestHeaders,
        @Min(30) @Max(86400) Integer intervalSeconds,
        @Min(1000) @Max(60000) Integer timeoutMs
) {}
