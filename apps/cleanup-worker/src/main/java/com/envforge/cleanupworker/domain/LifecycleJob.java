package com.envforge.cleanupworker.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LifecycleJob(
        UUID id,
        UUID environmentId,
        LifecycleAction action,
        LifecycleJobStatus status,
        int attemptCount,
        Integer targetRevision,
        String actorId,
        Instant createdAt,
        Instant updatedAt,
        String lastError
) {

    public LifecycleJob {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must not be negative");
        }
    }
}
