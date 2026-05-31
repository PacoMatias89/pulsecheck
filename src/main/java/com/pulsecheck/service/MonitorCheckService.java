package com.pulsecheck.service;

import com.pulsecheck.domain.entity.Monitor;
import com.pulsecheck.domain.entity.MonitorCheck;
import com.pulsecheck.domain.enums.CheckStatus;
import com.pulsecheck.dto.response.MonitorCheckResponse;
import com.pulsecheck.dto.response.MonitorStatsResponse;
import com.pulsecheck.repository.IncidentRepository;
import com.pulsecheck.repository.MonitorCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonitorCheckService {

    private final MonitorCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;

    public List<MonitorCheckResponse> getRecentChecks(UUID monitorId, int limit) {
        return checkRepository.findByMonitorIdOrderByCheckedAtDesc(
                monitorId, PageRequest.of(0, limit)
        ).stream().map(MonitorCheckResponse::from).toList();
    }

    public MonitorStatsResponse getStats(Monitor monitor) {
        var now = Instant.now();
        var ago24h = now.minus(24, ChronoUnit.HOURS);
        var ago7d  = now.minus(7,  ChronoUnit.DAYS);
        var ago30d = now.minus(30, ChronoUnit.DAYS);

        return new MonitorStatsResponse(
                uptimePct(monitor.getId(), ago24h),
                uptimePct(monitor.getId(), ago7d),
                uptimePct(monitor.getId(), ago30d),
                toLong(checkRepository.avgResponseTimeSince(monitor.getId(), ago24h)),
                checkRepository.countByMonitorSince(monitor.getId(), ago24h),
                incidentRepository.countOpenByWorkspace(monitor.getWorkspace().getId())
        );
    }

    public List<MonitorCheck> getChecksForChart(UUID monitorId, int hours) {
        var since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return checkRepository.findByMonitorSince(monitorId, since);
    }

    private double uptimePct(UUID monitorId, Instant since) {
        long total = checkRepository.countByMonitorSince(monitorId, since);
        if (total == 0) return 100.0;
        long up = checkRepository.countByMonitorAndStatusSince(monitorId, CheckStatus.UP, since);
        return Math.round((up * 100.0 / total) * 100.0) / 100.0;
    }

    private Long toLong(Double d) {
        return d == null ? null : d.longValue();
    }
}
