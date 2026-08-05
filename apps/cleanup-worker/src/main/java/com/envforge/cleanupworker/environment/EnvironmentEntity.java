package com.envforge.cleanupworker.environment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "environments")
public class EnvironmentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(nullable = false, unique = true, length = 63)
    private String namespace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnvironmentStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EnvironmentEntity() {
    }

    public EnvironmentEntity(
            UUID id,
            String name,
            String namespace,
            EnvironmentStatus status,
            Instant expiresAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.status = status;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getNamespace() { return namespace; }
    public EnvironmentStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markExpired(Instant now) {
        status = EnvironmentStatus.EXPIRED;
        updatedAt = now;
    }

    public void markDeleting(Instant now) {
        status = EnvironmentStatus.DELETING;
        updatedAt = now;
    }

    public void markDeleted(Instant now) {
        status = EnvironmentStatus.DELETED;
        updatedAt = now;
    }

    public void markReady(Instant now) {
        status = EnvironmentStatus.READY;
        updatedAt = now;
    }

    public void markFailed(Instant now) {
        status = EnvironmentStatus.FAILED;
        updatedAt = now;
    }
}
