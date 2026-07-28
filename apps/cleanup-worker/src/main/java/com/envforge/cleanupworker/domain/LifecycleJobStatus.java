package com.envforge.cleanupworker.domain;

public enum LifecycleJobStatus {
    QUEUED,
    RUNNING,
    RETRYING,
    SUCCEEDED,
    FAILED
}
