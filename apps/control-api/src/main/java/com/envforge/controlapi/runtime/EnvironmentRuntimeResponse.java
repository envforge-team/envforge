package com.envforge.controlapi.runtime;

import java.time.Instant;
import java.util.UUID;

public record EnvironmentRuntimeResponse(
    UUID environmentId,
    String environmentName,
    String namespace,
    boolean namespaceExists,
    String helmRelease,
    String helmStatus,
    String deploymentName,
    Integer desiredReplicas,
    Integer readyReplicas,
    String serviceName,
    Instant observedAt
) {
}
