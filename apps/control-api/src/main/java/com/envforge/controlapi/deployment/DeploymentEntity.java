// deployment/DeploymentEntity.java
package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "deployment")
public class DeploymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @Column(nullable = false)
    private String requestedVersion;

    private String imageTag;

    @Enumerated(EnumType.STRING)
    private DeploymentStatus status;

    private String triggeredBy;
    private Instant startedAt;
    private Instant finishedAt;
    private String failureReason;

    public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}

public EnvironmentEntity getEnvironment() {
    return environment;
}

public void setEnvironment(EnvironmentEntity environment) {
    this.environment = environment;
}

public String getRequestedVersion() {
    return requestedVersion;
}

public void setRequestedVersion(String requestedVersion) {
    this.requestedVersion = requestedVersion;
}

public String getImageTag() {
    return imageTag;
}

public void setImageTag(String imageTag) {
    this.imageTag = imageTag;
}

public DeploymentStatus getStatus() {
    return status;
}

public void setStatus(DeploymentStatus status) {
    this.status = status;
}

public String getTriggeredBy() {
    return triggeredBy;
}

public void setTriggeredBy(String triggeredBy) {
    this.triggeredBy = triggeredBy;
}

public Instant getStartedAt() {
    return startedAt;
}

public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
}

public Instant getFinishedAt() {
    return finishedAt;
}

public void setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
}

public String getFailureReason() {
    return failureReason;
}

public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
}

}