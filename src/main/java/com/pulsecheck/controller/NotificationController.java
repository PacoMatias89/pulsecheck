package com.pulsecheck.controller;

import com.pulsecheck.dto.request.CreateNotificationChannelRequest;
import com.pulsecheck.dto.response.NotificationChannelResponse;
import com.pulsecheck.service.AuthService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    @GetMapping("/channels")
    @Operation(summary = "List notification channels")
    public ResponseEntity<List<NotificationChannelResponse>> listChannels(
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var channels = notificationService.getChannels(user).stream()
                .map(NotificationChannelResponse::from).toList();
        return ResponseEntity.ok(channels);
    }

    @PostMapping("/channels")
    @Operation(summary = "Create notification channel")
    public ResponseEntity<NotificationChannelResponse> createChannel(
            @Valid @RequestBody CreateNotificationChannelRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        var channel = notificationService.createChannel(req, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NotificationChannelResponse.from(channel));
    }

    @DeleteMapping("/channels/{id}")
    @Operation(summary = "Delete notification channel")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        var user = authService.getByEmail(principal.getUsername());
        notificationService.deleteChannel(id, user);
        return ResponseEntity.noContent().build();
    }
}
