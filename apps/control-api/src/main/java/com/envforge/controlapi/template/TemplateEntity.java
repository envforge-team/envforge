package com.envforge.controlapi.template;

import java.time.Instant;
import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentTemplate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "environment_templates")
public class TemplateEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        unique = true,
        length = 50
    )
    private EnvironmentTemplate code;

    @Column(
        name = "display_name",
        nullable = false,
        length = 100
    )
    private String displayName;

    @Column(
        name = "image_repository",
        nullable = false,
        length = 255
    )
    private String imageRepository;

    @Column(
        name = "default_image_version",
        nullable = false,
        length = 100
    )
    private String defaultImageVersion;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TemplateEntity() {
    }

    public UUID getId() {
        return id;
    }

    public EnvironmentTemplate getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImageRepository() {
        return imageRepository;
    }

    public String getDefaultImageVersion() {
        return defaultImageVersion;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}