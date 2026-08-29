package com.envforge.controlapi.lifecycle;

import java.util.UUID;

public record LifecycleJobResponse(
    UUID id,
    UUID environmentId,
    String action,
    String status,
    int attemptCount
) {
}
