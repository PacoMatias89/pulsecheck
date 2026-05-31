package com.pulsecheck.controller;

import com.pulsecheck.dto.response.IncidentResponse;
import com.pulsecheck.service.AuthService;
import com.pulsecheck.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List all incidents")
    public ResponseEntity<List<IncidentResponse>> list(
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var incidents = incidentService.getIncidents(user).stream()
                .map(IncidentResponse::from).toList();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an incident")
    public ResponseEntity<IncidentResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(IncidentResponse.from(incidentService.getIncident(id, user)));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Manually resolve an incident")
    public ResponseEntity<IncidentResponse> resolve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(
                IncidentResponse.from(incidentService.resolveIncidentManually(id, user)));
    }
}
