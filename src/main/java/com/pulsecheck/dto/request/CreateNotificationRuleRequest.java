package com.pulsecheck.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateNotificationRuleRequest(
        @NotNull UUID channelId,
        Boolean notifyOnDown,
        Boolean notifyOnRecovery
) {}
