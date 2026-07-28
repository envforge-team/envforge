package com.envforge.cleanupworker.domain;

public enum EnvironmentStatus {
    PROVISIONING,
    READY,
    UPDATING,
    UPDATE_FAILED,
    ROLLING_BACK,
    ROLLBACK_FAILED,
    EXPIRED,
    DELETING,
    DELETE_FAILED,
    DELETED
}
