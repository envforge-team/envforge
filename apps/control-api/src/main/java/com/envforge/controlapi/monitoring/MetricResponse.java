package com.envforge.controlapi.monitoring;

import java.time.Instant;
import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentEntity;

public record MetricResponse(
    UUID environmentId,
    String environmentName,
    String namespace,
    HealthStatus status,
    Double cpuUsagePercent,
    Long memoryUsageBytes,
    Double requestRatePerSecond,
    Double errorRatePercent,
    Instant capturedAt
) {
    public static MetricResponse from(
        EnvironmentEntity environment,
        HealthSnapshotEntity snapshot
    ) {
        return new MetricResponse(
            environment.getId(),
            environment.getName(),
            environment.getNamespace(),
            snapshot.getStatus(),
            snapshot.getCpuUsagePercent(),
            snapshot.getMemoryUsageBytes(),
            snapshot.getRequestRatePerSecond(),
            snapshot.getErrorRatePercent(),
            snapshot.getCapturedAt()
        );
    }
}
