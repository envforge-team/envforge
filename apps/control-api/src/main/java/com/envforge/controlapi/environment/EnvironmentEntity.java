package com.envforge.controlapi.environment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    @Column(nullable = false, length = 50)
    private EnvironmentTemplate template;

    @Column(name = "image_version", nullable = false, length = 100)
    private String imageVersion;

    @Column(nullable = false)
    private int replicas;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_profile", nullable = false, length = 20)
    private ResourceProfile resourceProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnvironmentStatus status;

    @Column(name = "monitoring_enabled", nullable = false)
    private boolean monitoringEnabled;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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
        EnvironmentTemplate template,
        String imageVersion,
        int replicas,
        ResourceProfile resourceProfile,
        EnvironmentStatus status,
        boolean monitoringEnabled,
        String createdBy,
        Instant createdAt,
        Instant expiresAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.template = template;
        this.imageVersion = imageVersion;
        this.replicas = replicas;
        this.resourceProfile = resourceProfile;
        this.status = status;
        this.monitoringEnabled = monitoringEnabled;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public EnvironmentTemplate getTemplate() {
        return template;
    }

    public String getImageVersion() {
        return imageVersion;
    }

    public int getReplicas() {
        return replicas;
    }

    public ResourceProfile getResourceProfile() {
        return resourceProfile;
    }

    public EnvironmentStatus getStatus() {
        return status;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}