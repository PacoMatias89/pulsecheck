package com.pulsecheck.service;

import com.pulsecheck.domain.entity.Incident;
import com.pulsecheck.domain.entity.Monitor;
import com.pulsecheck.domain.entity.User;
import com.pulsecheck.domain.enums.IncidentStatus;
import com.pulsecheck.exception.ResourceNotFoundException;
import com.pulsecheck.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final WorkspaceService workspaceService;

    public List<Incident> getIncidents(User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        return incidentRepository.findByWorkspaceIdOrderByStartedAtDesc(workspace.getId());
    }

    public Incident getIncident(UUID incidentId, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        if (!incident.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResourceNotFoundException("Incident not found");
        }
        return incident;
    }

    @Transactional
    public Incident openIncident(Monitor monitor) {
        var existing = incidentRepository.findByMonitorIdAndStatus(
                monitor.getId(), IncidentStatus.OPEN);
        if (existing.isPresent()) {
            return existing.get();
        }
        var incident = Incident.builder()
                .monitor(monitor)
                .workspace(monitor.getWorkspace())
                .title(monitor.getName() + " is down")
                .build();
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident resolveIncident(Monitor monitor) {
        return incidentRepository.findByMonitorIdAndStatus(monitor.getId(), IncidentStatus.OPEN)
                .map(incident -> {
                    incident.setStatus(IncidentStatus.RESOLVED);
                    incident.setResolvedAt(Instant.now());
                    incident.setDurationSeconds(
                            Instant.now().getEpochSecond() - incident.getStartedAt().getEpochSecond());
                    return incidentRepository.save(incident);
                })
                .orElse(null);
    }

    @Transactional
    public Incident resolveIncidentManually(UUID incidentId, User user) {
        var incident = getIncident(incidentId, user);
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            return incident;
        }
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        incident.setDurationSeconds(
                Instant.now().getEpochSecond() - incident.getStartedAt().getEpochSecond());
        return incidentRepository.save(incident);
    }
}
