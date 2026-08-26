package com.envforge.controlapi.provisioning;

import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentStatus;

public class EnvironmentRetryNotAllowedException
    extends RuntimeException {

    public EnvironmentRetryNotAllowedException(
        UUID environmentId,
        EnvironmentStatus currentStatus
    ) {
        super(
            "Environment "
                + environmentId
                + " cannot be retried from status "
                + currentStatus
        );
    }
}