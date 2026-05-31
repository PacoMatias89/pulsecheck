package com.pulsecheck.controller;

import com.pulsecheck.dto.response.StatusPageResponse;
import com.pulsecheck.service.StatusPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/status")
@RequiredArgsConstructor
@Tag(name = "Public Status Pages")
public class PublicStatusController {

    private final StatusPageService statusPageService;

    @GetMapping("/{slug}")
    @Operation(summary = "Get public status page by slug (no auth required)")
    public ResponseEntity<StatusPageResponse> getPublicPage(@PathVariable String slug) {
        return ResponseEntity.ok(statusPageService.getPublicStatusPage(slug));
    }
}
