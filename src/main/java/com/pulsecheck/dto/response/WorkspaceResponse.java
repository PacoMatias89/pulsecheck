package com.pulsecheck.dto.response;

import com.pulsecheck.domain.entity.Workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(UUID id, String name, String slug, Instant createdAt) {
    public static WorkspaceResponse from(Workspace w) {
        return new WorkspaceResponse(w.getId(), w.getName(), w.getSlug(), w.getCreatedAt());
    }
}
