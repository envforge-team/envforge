package com.envforge.cleanupworker.persistence.entity;

import com.envforge.cleanupworker.domain.AuditResult;
import com.envforge.cleanupworker.domain.LifecycleAction;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lifecycle_audit")
public class LifecycleAuditEntity {

    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LifecycleAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditResult result;

    @Column(length = 4000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LifecycleAuditEntity() {
    }

    public LifecycleAuditEntity(
            UUID id,
            UUID environmentId,
            UUID jobId,
            String actorId,
            LifecycleAction action,
            AuditResult result,
            String details,
            Instant createdAt
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.jobId = jobId;
        this.actorId = actorId;
        this.action = action;
        this.result = result;
        this.details = details;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getEnvironmentId() { return environmentId; }
    public UUID getJobId() { return jobId; }
    public String getActorId() { return actorId; }
    public LifecycleAction getAction() { return action; }
    public AuditResult getResult() { return result; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
