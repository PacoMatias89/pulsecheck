package com.pulsecheck.controller;

import com.pulsecheck.domain.enums.MonitorStatus;
import com.pulsecheck.dto.response.DashboardStatsResponse;
import com.pulsecheck.repository.IncidentRepository;
import com.pulsecheck.repository.MonitorRepository;
import com.pulsecheck.service.AuthService;
import com.pulsecheck.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final MonitorRepository monitorRepository;
    private final IncidentRepository incidentRepository;
    private final WorkspaceService workspaceService;
    private final AuthService authService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard overview stats")
    public ResponseEntity<DashboardStatsResponse> stats(
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var workspace = workspaceService.getDefaultWorkspace(user);
        var monitors = monitorRepository.findByWorkspaceId(workspace.getId());

        long total   = monitors.size();
        long up      = monitors.stream().filter(m -> m.getStatus() == MonitorStatus.UP).count();
        long down    = monitors.stream().filter(m -> m.getStatus() == MonitorStatus.DOWN).count();
        long pending = monitors.stream().filter(m -> m.getStatus() == MonitorStatus.PENDING).count();
        long open    = incidentRepository.countOpenByWorkspace(workspace.getId());
        double uptime = total == 0 ? 100.0 : Math.round((up * 100.0 / total) * 100.0) / 100.0;

        return ResponseEntity.ok(new DashboardStatsResponse(total, up, down, pending, open, uptime));
    }
}
