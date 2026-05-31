package com.pulsecheck.dto.response;

public record MonitorStatsResponse(
        double uptimePercentage24h,
        double uptimePercentage7d,
        double uptimePercentage30d,
        Long avgResponseTimeMs,
        long totalChecks24h,
        long openIncidents
) {}
