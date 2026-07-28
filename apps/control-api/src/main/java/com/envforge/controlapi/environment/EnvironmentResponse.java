package com.envforge.controlapi.environment;

import java.time.Instant;
import java.util.UUID;

public record EnvironmentResponse(
    UUID id,
    String name,
    String namespace,
    EnvironmentTemplate template,
    String imageVersion,
    int replicas,
    ResourceProfile resourceProfile,
    EnvironmentStatus status,
    boolean monitoringEnabled,
    String createdBy,
    Instant createdAt,
    Instant expiresAt,
    Instant updatedAt
) {
    public static EnvironmentResponse from(
        EnvironmentEntity environment
    ) {
        return new EnvironmentResponse(
            environment.getId(),
            environment.getName(),
            environment.getNamespace(),
            environment.getTemplate(),
            environment.getImageVersion(),
            environment.getReplicas(),
            environment.getResourceProfile(),
            environment.getStatus(),
            environment.isMonitoringEnabled(),
            environment.getCreatedBy(),
            environment.getCreatedAt(),
            environment.getExpiresAt(),
            environment.getUpdatedAt()
        );
    }
}