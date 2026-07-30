package com.envforge.controlapi.deployment;

import java.time.Instant;

public record DeploymentResponse(
    Long id,
    Long environmentId,
    String requestedVersion,
    String imageTag,
    DeploymentStatus status,
    String triggeredBy,
    Instant startedAt,
    Instant finishedAt,
    String failureReason
) {}