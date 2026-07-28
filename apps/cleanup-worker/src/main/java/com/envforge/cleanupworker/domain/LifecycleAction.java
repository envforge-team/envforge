package com.envforge.cleanupworker.domain;

public enum LifecycleAction {
    EXPIRE,
    DELETE,
    ROLLBACK,
    RETRY_DELETE,
    RETRY_ROLLBACK,
    EXTEND_LIFETIME
}
