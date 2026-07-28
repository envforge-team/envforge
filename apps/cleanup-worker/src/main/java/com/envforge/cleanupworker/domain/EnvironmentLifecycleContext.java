package com.envforge.cleanupworker.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EnvironmentLifecycleContext(
        UUID environmentId,
        EnvironmentStatus status,
        String namespaceName,
        String helmReleaseName,
        Integer currentRevision,
        Integer previousSuccessfulRevision,
        Instant expiresAt,
        long version
) {

    public EnvironmentLifecycleContext {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
