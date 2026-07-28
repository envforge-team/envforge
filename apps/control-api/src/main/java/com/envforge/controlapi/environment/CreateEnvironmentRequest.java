package com.envforge.controlapi.environment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEnvironmentRequest(

    @NotBlank(message = "Environment name is required")
    @Size(
        min = 3,
        max = 40,
        message = "Environment name must contain between 3 and 40 characters"
    )
    @Pattern(
        regexp = "^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$",
        message = "Environment name must contain lowercase letters, numbers and hyphens"
    )
    String name,

    @NotNull(message = "Template is required")
    EnvironmentTemplate template,

    @NotBlank(message = "Image version is required")
    @Size(
        max = 100,
        message = "Image version cannot exceed 100 characters"
    )
    String imageVersion,

    @Min(value = 1, message = "At least one replica is required")
    @Max(value = 5, message = "A maximum of five replicas is allowed")
    int replicas,

    @NotNull(message = "Resource profile is required")
    ResourceProfile resourceProfile,

    @Min(value = 1, message = "Lifetime must be at least one hour")
    @Max(value = 24, message = "Lifetime cannot exceed 24 hours")
    int lifetimeHours,

    boolean monitoringEnabled
) {
}