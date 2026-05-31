package com.pulsecheck.dto.response;

public record DashboardStatsResponse(
        long totalMonitors,
        long monitorsUp,
        long monitorsDown,
        long monitorsPending,
        long openIncidents,
        double overallUptimePercentage
) {}
