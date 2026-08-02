package com.envforge.controlapi.deployment;

import java.util.UUID;

public class ConcurrentRolloutException extends RuntimeException {
    public ConcurrentRolloutException(UUID environmentId) {
        super("A rollout is already in progress for environment: " + environmentId);
    }
}