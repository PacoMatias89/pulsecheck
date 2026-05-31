package com.pulsecheck.controller;

import com.pulsecheck.dto.request.CreateMonitorRequest;
import com.pulsecheck.dto.request.CreateNotificationRuleRequest;
import com.pulsecheck.dto.request.UpdateMonitorRequest;
import com.pulsecheck.dto.response.MonitorCheckResponse;
import com.pulsecheck.dto.response.MonitorResponse;
import com.pulsecheck.dto.response.MonitorStatsResponse;
import com.pulsecheck.service.AuthService;
import com.pulsecheck.service.MonitorCheckService;
import com.pulsecheck.service.MonitorService;
import com.pulsecheck.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitors")
@RequiredArgsConstructor
@Tag(name = "Monitors")
public class MonitorController {

    private final MonitorService monitorService;
    private final MonitorCheckService checkService;
    private final NotificationService notificationService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List all monitors")
    public ResponseEntity<List<MonitorResponse>> list(@AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var monitors = monitorService.getMonitors(user).stream()
                .map(MonitorResponse::from).toList();
        return ResponseEntity.ok(monitors);
    }

    @PostMapping
    @Operation(summary = "Create a monitor")
    public ResponseEntity<MonitorResponse> create(
            @Valid @RequestBody CreateMonitorRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var monitor = monitorService.createMonitor(req, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(MonitorResponse.from(monitor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a monitor")
    public ResponseEntity<MonitorResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(MonitorResponse.from(monitorService.getMonitor(id, user)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a monitor")
    public ResponseEntity<MonitorResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMonitorRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(MonitorResponse.from(monitorService.updateMonitor(id, req, user)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a monitor")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        monitorService.deleteMonitor(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause a monitor")
    public ResponseEntity<MonitorResponse> pause(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(MonitorResponse.from(monitorService.pauseMonitor(id, user)));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume a paused monitor")
    public ResponseEntity<MonitorResponse> resume(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(MonitorResponse.from(monitorService.resumeMonitor(id, user)));
    }

    @GetMapping("/{id}/checks")
    @Operation(summary = "Get recent check results")
    public ResponseEntity<List<MonitorCheckResponse>> checks(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        monitorService.getMonitor(id, user);
        return ResponseEntity.ok(checkService.getRecentChecks(id, Math.min(limit, 500)));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get monitor uptime stats")
    public ResponseEntity<MonitorStatsResponse> stats(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var monitor = monitorService.getMonitor(id, user);
        return ResponseEntity.ok(checkService.getStats(monitor));
    }

    @PostMapping("/{id}/notification-rules")
    @Operation(summary = "Add notification rule to monitor")
    public ResponseEntity<Void> addRule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateNotificationRuleRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        notificationService.createRule(id, req, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/notification-rules/{channelId}")
    @Operation(summary = "Remove notification rule from monitor")
    public ResponseEntity<Void> removeRule(
            @PathVariable UUID id,
            @PathVariable UUID channelId,
            @AuthenticationPrincipal UserDetails principal) {
        authService.getByEmail(principal.getUsername());
        notificationService.deleteRule(id, channelId);
        return ResponseEntity.noContent().build();
    }
}
