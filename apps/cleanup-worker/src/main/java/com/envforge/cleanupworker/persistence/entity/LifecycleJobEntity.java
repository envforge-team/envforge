package com.envforge.cleanupworker.persistence.entity;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lifecycle_job")
public class LifecycleJobEntity {

    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LifecycleAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LifecycleJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "target_revision")
    private Integer targetRevision;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "namespace_name")
    private String namespaceName;

    @Column(name = "helm_release_name")
    private String helmReleaseName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Version
    @Column(nullable = false)
    private long version;

    protected LifecycleJobEntity() {
    }

    public LifecycleJobEntity(
            UUID id,
            UUID environmentId,
            LifecycleAction action,
            LifecycleJobStatus status,
            int attemptCount,
            Integer targetRevision,
            String actorId,
            String namespaceName,
            String helmReleaseName,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.action = action;
        this.status = status;
        this.attemptCount = attemptCount;
        this.targetRevision = targetRevision;
        this.actorId = actorId;
        this.namespaceName = namespaceName;
        this.helmReleaseName = helmReleaseName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getEnvironmentId() { return environmentId; }
    public LifecycleAction getAction() { return action; }
    public LifecycleJobStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Integer getTargetRevision() { return targetRevision; }
    public String getActorId() { return actorId; }
    public String getNamespaceName() { return namespaceName; }
    public String getHelmReleaseName() { return helmReleaseName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getLastError() { return lastError; }
    public long getVersion() { return version; }

    public void markRunning(Instant now) {
        status = LifecycleJobStatus.RUNNING;
        attemptCount++;
        startedAt = now;
        updatedAt = now;
        nextRetryAt = null;
        lastError = null;
    }

    public void markSucceeded(Instant now) {
        status = LifecycleJobStatus.SUCCEEDED;
        finishedAt = now;
        updatedAt = now;
        nextRetryAt = null;
        lastError = null;
    }

    public void markRetrying(Instant now, Instant retryAt, String error) {
        status = LifecycleJobStatus.RETRYING;
        updatedAt = now;
        nextRetryAt = retryAt;
        lastError = truncate(error);
    }

    public void markFailed(Instant now, String error) {
        status = LifecycleJobStatus.FAILED;
        finishedAt = now;
        updatedAt = now;
        nextRetryAt = null;
        lastError = truncate(error);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }
}
