package com.pulsecheck.scheduler;

import com.pulsecheck.domain.entity.Monitor;
import com.pulsecheck.domain.entity.MonitorCheck;
import com.pulsecheck.domain.enums.CheckStatus;
import com.pulsecheck.domain.enums.MonitorStatus;
import com.pulsecheck.domain.enums.MonitorType;
import com.pulsecheck.repository.MonitorCheckRepository;
import com.pulsecheck.repository.MonitorRepository;
import com.pulsecheck.service.IncidentService;
import com.pulsecheck.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCheckExecutor {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository checkRepository;
    private final IncidentService incidentService;
    private final NotificationService notificationService;

    @Value("${app.monitor.max-consecutive-failures:3}")
    private int maxConsecutiveFailures;

    @Async("monitorExecutor")
    @Transactional
    public void executeCheck(Monitor monitor) {
        var result = performHttpCheck(monitor);
        saveCheckResult(monitor, result);
        updateMonitorStatus(monitor, result);
    }

    private CheckResult performHttpCheck(Monitor monitor) {
        var start = System.currentTimeMillis();
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.min(monitor.getTimeoutMs(), 30000)))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(monitor.getUrl()))
                    .timeout(Duration.ofMillis(monitor.getTimeoutMs()));

            monitor.getRequestHeaders().forEach(requestBuilder::header);

            requestBuilder.method(monitor.getMethod(),
                    HttpRequest.BodyPublishers.noBody());

            var response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            var elapsed = System.currentTimeMillis() - start;
            var statusCode = response.statusCode();
            var bodyCheck = checkBodyKeyword(monitor, response.body());

            if (statusCode == monitor.getExpectedStatusCode() && bodyCheck) {
                return CheckResult.up(elapsed, statusCode);
            } else {
                var reason = !bodyCheck
                        ? "Keyword '" + monitor.getKeywordContains() + "' not found"
                        : "Expected status " + monitor.getExpectedStatusCode() + " but got " + statusCode;
                return CheckResult.down(elapsed, statusCode, reason);
            }

        } catch (Exception e) {
            var elapsed = System.currentTimeMillis() - start;
            return CheckResult.down(elapsed, null, summarizeError(e));
        }
    }

    private boolean checkBodyKeyword(Monitor monitor, String body) {
        if (monitor.getType() != MonitorType.KEYWORD || monitor.getKeywordContains() == null) {
            return true;
        }
        return body != null && body.contains(monitor.getKeywordContains());
    }

    private void saveCheckResult(Monitor monitor, CheckResult result) {
        var check = MonitorCheck.builder()
                .monitor(monitor)
                .status(result.status())
                .responseTimeMs(result.responseTimeMs())
                .statusCode(result.statusCode())
                .errorMessage(result.errorMessage())
                .checkedAt(Instant.now())
                .build();
        checkRepository.save(check);
    }

    @Transactional
    protected void updateMonitorStatus(Monitor monitor, CheckResult result) {
        var previous = monitor.getStatus();

        monitor.setLastCheckedAt(Instant.now());

        if (result.status() == CheckStatus.UP) {
            boolean wasDown = previous == MonitorStatus.DOWN;
            monitor.setConsecutiveFailures(0);
            monitor.setStatus(MonitorStatus.UP);
            if (wasDown) {
                monitor.setLastStatusChangeAt(Instant.now());
                monitorRepository.save(monitor);
                incidentService.resolveIncident(monitor);
                notificationService.sendRecoveryAlert(monitor);
            } else {
                monitorRepository.save(monitor);
            }
        } else {
            int failures = monitor.getConsecutiveFailures() + 1;
            monitor.setConsecutiveFailures(failures);

            if (failures >= maxConsecutiveFailures) {
                boolean wasUp = previous == MonitorStatus.UP || previous == MonitorStatus.PENDING;
                monitor.setStatus(MonitorStatus.DOWN);
                if (wasUp) {
                    monitor.setLastStatusChangeAt(Instant.now());
                    monitorRepository.save(monitor);
                    var incident = incidentService.openIncident(monitor);
                    notificationService.sendDownAlert(monitor, incident);
                } else {
                    monitorRepository.save(monitor);
                }
            } else {
                monitorRepository.save(monitor);
            }
        }

        log.debug("Monitor {} [{}] | Status: {} | Response: {}ms",
                monitor.getName(), monitor.getId(),
                result.status(), result.responseTimeMs());
    }

    private String summarizeError(Exception e) {
        var msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        if (msg.length() > 200) return msg.substring(0, 200) + "...";
        return msg;
    }

    record CheckResult(CheckStatus status, long responseTimeMs,
                       Integer statusCode, String errorMessage) {
        static CheckResult up(long ms, int code) {
            return new CheckResult(CheckStatus.UP, ms, code, null);
        }
        static CheckResult down(long ms, Integer code, String error) {
            return new CheckResult(CheckStatus.DOWN, ms, code, error);
        }
    }
}
