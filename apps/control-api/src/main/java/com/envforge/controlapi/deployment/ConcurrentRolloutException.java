package com.envforge.controlapi.deployment;

public class ConcurrentRolloutException extends RuntimeException {
    public ConcurrentRolloutException(Long environmentId) {
        super("A rollout is already in progress for environment: " + environmentId);
    }
}

