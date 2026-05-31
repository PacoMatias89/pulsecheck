package com.pulsecheck.dto.response;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(status, error, message, Instant.now(), Map.of());
    }

    public static ApiErrorResponse withFields(int status, String error, String message,
                                               Map<String, String> fieldErrors) {
        return new ApiErrorResponse(status, error, message, Instant.now(), fieldErrors);
    }
}
