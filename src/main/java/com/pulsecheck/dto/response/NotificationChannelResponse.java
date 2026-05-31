package com.pulsecheck.dto.response;

import com.pulsecheck.domain.entity.NotificationChannel;
import com.pulsecheck.domain.enums.NotificationChannelType;

import java.time.Instant;
import java.util.UUID;

public record NotificationChannelResponse(
        UUID id,
        String name,
        NotificationChannelType type,
        boolean active,
        Instant createdAt
) {
    public static NotificationChannelResponse from(NotificationChannel c) {
        return new NotificationChannelResponse(c.getId(), c.getName(), c.getType(),
                c.isActive(), c.getCreatedAt());
    }
}
