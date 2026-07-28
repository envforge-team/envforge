package com.envforge.controlapi.environment;

public enum EnvironmentStatus {
    REQUESTED,
    PROVISIONING,
    DEPLOYING,
    READY,
    DEGRADED,
    FAILED,
    DELETING,
    DELETED,
    EXPIRED
}