package com.envforge.cleanupworker.domain;

public enum AuditResult {
    REQUESTED,
    RUNNING,
    RETRYING,
    SUCCEEDED,
    FAILED
}
