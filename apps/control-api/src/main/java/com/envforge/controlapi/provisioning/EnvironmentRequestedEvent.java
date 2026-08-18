package com.envforge.controlapi.provisioning;

import java.util.UUID;

public record EnvironmentRequestedEvent(
    UUID environmentId
) {
}