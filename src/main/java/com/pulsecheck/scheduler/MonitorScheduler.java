package com.pulsecheck.scheduler;

import com.pulsecheck.repository.MonitorCheckRepository;
import com.pulsecheck.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository checkRepository;
    private final HttpCheckExecutor executor;

    @Value("${app.monitor.check-interval-seconds:30}")
    private int schedulerIntervalSeconds;

    @Scheduled(fixedDelayString = "${app.monitor.check-interval-seconds:30}000")
    public void runDueChecks() {
        var cutoff = Instant.now().minusSeconds(schedulerIntervalSeconds);

        var dueMonitors = monitorRepository.findMonitorsDue(cutoff);
        if (dueMonitors.isEmpty()) return;

        log.debug("Scheduling {} monitors for check", dueMonitors.size());
        dueMonitors.forEach(executor::executeCheck);
    }

    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanOldChecks() {
        var cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        checkRepository.deleteByCheckedAtBefore(cutoff);
        log.info("Cleaned monitor checks older than 90 days");
    }
}
