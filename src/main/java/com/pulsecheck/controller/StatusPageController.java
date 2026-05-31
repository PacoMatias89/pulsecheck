package com.pulsecheck.controller;

import com.pulsecheck.dto.request.AddMonitorToPageRequest;
import com.pulsecheck.dto.request.CreateStatusPageRequest;
import com.pulsecheck.dto.response.StatusPageResponse;
import com.pulsecheck.dto.response.WorkspaceResponse;
import com.pulsecheck.service.AuthService;
import com.pulsecheck.service.StatusPageService;
import com.pulsecheck.service.WorkspaceService;
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
@RequestMapping("/api/status-pages")
@RequiredArgsConstructor
@Tag(name = "Status Pages")
public class StatusPageController {

    private final StatusPageService statusPageService;
    private final AuthService authService;
    private final WorkspaceService workspaceService;

    @GetMapping
    @Operation(summary = "List status pages")
    public ResponseEntity<List<StatusPageResponse>> list(
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var pages = statusPageService.getStatusPages(user).stream()
                .map(p -> statusPageService.getStatusPage(p.getId(), user)).toList();
        return ResponseEntity.ok(pages);
    }

    @PostMapping
    @Operation(summary = "Create a status page")
    public ResponseEntity<StatusPageResponse> create(
            @Valid @RequestBody CreateStatusPageRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var page = statusPageService.createStatusPage(req, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statusPageService.getStatusPage(page.getId(), user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a status page")
    public ResponseEntity<StatusPageResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(statusPageService.getStatusPage(id, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a status page")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        statusPageService.deleteStatusPage(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/monitors")
    @Operation(summary = "Add monitor to status page")
    public ResponseEntity<Void> addMonitor(
            @PathVariable UUID id,
            @Valid @RequestBody AddMonitorToPageRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        statusPageService.addMonitorToPage(id, req, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/monitors/{monitorId}")
    @Operation(summary = "Remove monitor from status page")
    public ResponseEntity<Void> removeMonitor(
            @PathVariable UUID id,
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        statusPageService.removeMonitorFromPage(id, monitorId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/workspaces")
    @Operation(summary = "Get user workspaces")
    public ResponseEntity<List<WorkspaceResponse>> workspaces(
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var workspaces = workspaceService.getUserWorkspaces(user.getId()).stream()
                .map(WorkspaceResponse::from).toList();
        return ResponseEntity.ok(workspaces);
    }
}
