package com.pulsecheck.dto.response;

import com.pulsecheck.domain.entity.User;
import com.pulsecheck.domain.enums.UserPlan;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserPlan plan,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getPlan(), user.getCreatedAt());
    }
}
