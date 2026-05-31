package com.pulsecheck.dto.response;

import com.pulsecheck.domain.entity.StatusPage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StatusPageResponse(
        UUID id,
        String name,
        String slug,
        String customDomain,
        String description,
        boolean publicPage,
        String logoUrl,
        String primaryColor,
        Instant createdAt,
        List<StatusPageMonitorResponse> monitors
) {
    public record StatusPageMonitorResponse(
            UUID id,
            UUID monitorId,
            String displayName,
            String status,
            int orderIndex
    ) {}

    public static StatusPageResponse from(StatusPage p) {
        return new StatusPageResponse(
                p.getId(), p.getName(), p.getSlug(), p.getCustomDomain(),
                p.getDescription(), p.isPublicPage(), p.getLogoUrl(),
                p.getPrimaryColor(), p.getCreatedAt(), List.of()
        );
    }
}
