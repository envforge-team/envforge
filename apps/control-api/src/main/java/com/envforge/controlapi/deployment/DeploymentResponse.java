package com.envforge.controlapi.deployment;

import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
    Long id,
    UUID environmentId,
    String requestedVersion,
    String imageTag,
    DeploymentStatus status,
    String triggeredBy,
    Instant startedAt,
    Instant finishedAt,
    String failureReason
) {
    public static DeploymentResponse fromEntity(DeploymentEntity entity) {
        return new DeploymentResponse(
            entity.getId(),
            entity.getEnvironment().getId(),
            entity.getRequestedVersion(),
            entity.getImageTag(),
            entity.getStatus(),
            entity.getTriggeredBy(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getFailureReason()
        );
    }
}