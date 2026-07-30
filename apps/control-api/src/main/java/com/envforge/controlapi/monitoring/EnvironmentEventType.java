package com.envforge.controlapi.monitoring;

public enum EnvironmentEventType {
    CREATED,
    DEPLOYMENT_STARTED,
    DEPLOYMENT_SUCCEEDED,
    DEPLOYMENT_FAILED,
    POD_RESTARTED,
    HEALTH_DEGRADED,
    HEALTH_RECOVERED,
    EXPIRED,
    DELETED
}
