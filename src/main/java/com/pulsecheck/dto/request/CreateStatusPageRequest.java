package com.pulsecheck.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStatusPageRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
        String description,
        Boolean isPublic,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String primaryColor,
        String logoUrl
) {}
