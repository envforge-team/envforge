package com.envforge.controlapi.monitoring;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "health_snapshots")
public class HealthSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthStatus status;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "memory_usage_bytes")
    private Long memoryUsageBytes;

    @Column(name = "request_rate_per_second")
    private Double requestRatePerSecond;

    @Column(name = "error_rate_percent")
    private Double errorRatePercent;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    protected HealthSnapshotEntity() {
    }

    public HealthSnapshotEntity(
        UUID id,
        UUID environmentId,
        HealthStatus status,
        Double cpuUsagePercent,
        Long memoryUsageBytes,
        Double requestRatePerSecond,
        Double errorRatePercent,
        Instant capturedAt
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.status = status;
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsageBytes = memoryUsageBytes;
        this.requestRatePerSecond = requestRatePerSecond;
        this.errorRatePercent = errorRatePercent;
        this.capturedAt = capturedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public Long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }

    public Double getRequestRatePerSecond() {
        return requestRatePerSecond;
    }

    public Double getErrorRatePercent() {
        return errorRatePercent;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }
}
