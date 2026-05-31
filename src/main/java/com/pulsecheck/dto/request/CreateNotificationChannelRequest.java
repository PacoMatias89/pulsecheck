package com.pulsecheck.dto.request;

import com.pulsecheck.domain.enums.NotificationChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateNotificationChannelRequest(
        @NotBlank String name,
        @NotNull NotificationChannelType type,
        @NotNull Map<String, String> config
) {}
