package com.pulsecheck.service;

import com.pulsecheck.domain.entity.Monitor;
import com.pulsecheck.domain.entity.User;
import com.pulsecheck.domain.enums.MonitorStatus;
import com.pulsecheck.domain.enums.MonitorType;
import com.pulsecheck.dto.request.CreateMonitorRequest;
import com.pulsecheck.dto.request.UpdateMonitorRequest;
import com.pulsecheck.exception.BusinessException;
import com.pulsecheck.exception.ResourceNotFoundException;
import com.pulsecheck.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final WorkspaceService workspaceService;

    public List<Monitor> getMonitors(User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        return monitorRepository.findByWorkspaceId(workspace.getId());
    }

    public Monitor getMonitor(UUID monitorId, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        return monitorRepository.findByIdAndWorkspaceId(monitorId, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
    }

    @Transactional
    public Monitor createMonitor(CreateMonitorRequest req, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);

        int currentCount = monitorRepository.countByWorkspaceId(workspace.getId());
        if (currentCount >= user.getPlan().maxMonitors) {
            throw new BusinessException(
                    "Monitor limit reached for your plan (" + user.getPlan().maxMonitors + "). Please upgrade.");
        }

        int interval = req.intervalSeconds() != null ? req.intervalSeconds() : 300;
        if (interval < user.getPlan().minIntervalSeconds) {
            throw new BusinessException(
                    "Minimum check interval for your plan is " + user.getPlan().minIntervalSeconds + " seconds.");
        }

        var monitor = Monitor.builder()
                .workspace(workspace)
                .name(req.name())
                .url(normalizeUrl(req.url(), req.type()))
                .type(req.type() != null ? req.type() : MonitorType.HTTPS)
                .method(req.method() != null ? req.method() : "GET")
                .expectedStatusCode(req.expectedStatusCode() != null ? req.expectedStatusCode() : 200)
                .keywordContains(req.keywordContains())
                .requestHeaders(req.requestHeaders() != null ? req.requestHeaders() : Map.of())
                .intervalSeconds(interval)
                .timeoutMs(req.timeoutMs() != null ? req.timeoutMs() : 30000)
                .build();

        return monitorRepository.save(monitor);
    }

    @Transactional
    public Monitor updateMonitor(UUID monitorId, UpdateMonitorRequest req, User user) {
        var monitor = getMonitor(monitorId, user);

        if (req.name() != null) monitor.setName(req.name());
        if (req.url() != null) monitor.setUrl(normalizeUrl(req.url(), req.type()));
        if (req.type() != null) monitor.setType(req.type());
        if (req.method() != null) monitor.setMethod(req.method());
        if (req.expectedStatusCode() != null) monitor.setExpectedStatusCode(req.expectedStatusCode());
        if (req.keywordContains() != null) monitor.setKeywordContains(req.keywordContains());
        if (req.requestHeaders() != null) monitor.setRequestHeaders(req.requestHeaders());
        if (req.intervalSeconds() != null) {
            if (req.intervalSeconds() < user.getPlan().minIntervalSeconds) {
                throw new BusinessException(
                        "Minimum check interval for your plan is " + user.getPlan().minIntervalSeconds + " seconds.");
            }
            monitor.setIntervalSeconds(req.intervalSeconds());
        }
        if (req.timeoutMs() != null) monitor.setTimeoutMs(req.timeoutMs());

        return monitorRepository.save(monitor);
    }

    @Transactional
    public void deleteMonitor(UUID monitorId, User user) {
        var monitor = getMonitor(monitorId, user);
        monitorRepository.delete(monitor);
    }

    @Transactional
    public Monitor pauseMonitor(UUID monitorId, User user) {
        var monitor = getMonitor(monitorId, user);
        monitor.setActive(false);
        monitor.setStatus(MonitorStatus.PAUSED);
        return monitorRepository.save(monitor);
    }

    @Transactional
    public Monitor resumeMonitor(UUID monitorId, User user) {
        var monitor = getMonitor(monitorId, user);
        monitor.setActive(true);
        monitor.setStatus(MonitorStatus.PENDING);
        monitor.setLastCheckedAt(null);
        return monitorRepository.save(monitor);
    }

    private String normalizeUrl(String url, MonitorType type) {
        if (url == null) return url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return (type == MonitorType.HTTP ? "http://" : "https://") + url;
        }
        return url;
    }
}
