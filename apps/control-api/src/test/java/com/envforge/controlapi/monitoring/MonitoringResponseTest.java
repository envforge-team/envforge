package com.envforge.controlapi.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;

class MonitoringResponseTest {

    @Test
    void shouldCreateMetricResponseFromEntities() {
        UUID environmentId = UUID.randomUUID();
        Instant createdAt = Instant.parse(
            "2026-07-30T09:00:00Z"
        );
        Instant capturedAt = Instant.parse(
            "2026-07-30T10:40:00Z"
        );

        EnvironmentEntity environment =
            new EnvironmentEntity(
                environmentId,
                "reliability-demo",
                "env-reliability-demo",
                EnvironmentTemplate.RELIABILITY_API,
                "0.1.0",
                2,
                ResourceProfile.SMALL,
                EnvironmentStatus.READY,
                true,
                "developer@envforge.local",
                createdAt,
                createdAt.plusSeconds(7200),
                createdAt
            );

        HealthSnapshotEntity snapshot =
            new HealthSnapshotEntity(
                UUID.randomUUID(),
                environmentId,
                HealthStatus.HEALTHY,
                23.7,
                384_827_392L,
                18.4,
                0.6,
                capturedAt
            );

        MetricResponse response =
            MetricResponse.from(environment, snapshot);

        assertEquals(
            environmentId,
            response.environmentId()
        );
        assertEquals(
            "reliability-demo",
            response.environmentName()
        );
        assertEquals(
            "env-reliability-demo",
            response.namespace()
        );
        assertEquals(
            HealthStatus.HEALTHY,
            response.status()
        );
        assertEquals(
            23.7,
            response.cpuUsagePercent()
        );
        assertEquals(
            384_827_392L,
            response.memoryUsageBytes()
        );
        assertEquals(
            18.4,
            response.requestRatePerSecond()
        );
        assertEquals(
            0.6,
            response.errorRatePercent()
        );
        assertEquals(
            capturedAt,
            response.capturedAt()
        );
    }

    @Test
    void shouldPreserveUnavailableMetricsAsNull() {
        UUID environmentId = UUID.randomUUID();
        Instant timestamp = Instant.parse(
            "2026-07-30T10:45:00Z"
        );

        EnvironmentEntity environment =
            new EnvironmentEntity(
                environmentId,
                "partial-demo",
                "env-partial-demo",
                EnvironmentTemplate.STATIC_WEB,
                "0.1.0",
                1,
                ResourceProfile.SMALL,
                EnvironmentStatus.DEGRADED,
                true,
                "developer@envforge.local",
                timestamp,
                timestamp.plusSeconds(7200),
                timestamp
            );

        HealthSnapshotEntity snapshot =
            new HealthSnapshotEntity(
                UUID.randomUUID(),
                environmentId,
                HealthStatus.DEGRADED,
                null,
                null,
                5.2,
                null,
                timestamp
            );

        MetricResponse response =
            MetricResponse.from(environment, snapshot);

        assertNull(response.cpuUsagePercent());
        assertNull(response.memoryUsageBytes());
        assertEquals(
            5.2,
            response.requestRatePerSecond()
        );
        assertNull(response.errorRatePercent());
    }

    @Test
    void shouldCreateEventResponseFromEntity() {
        UUID eventId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        Instant occurredAt = Instant.parse(
            "2026-07-30T10:39:12Z"
        );

        EnvironmentEventEntity event =
            new EnvironmentEventEntity(
                eventId,
                environmentId,
                EnvironmentEventType.POD_RESTARTED,
                EventSeverity.WARNING,
                "kubernetes",
                "The workload pod restarted.",
                occurredAt
            );

        EventResponse response =
            EventResponse.from(event);

        assertEquals(eventId, response.id());
        assertEquals(
            environmentId,
            response.environmentId()
        );
        assertEquals(
            EnvironmentEventType.POD_RESTARTED,
            response.eventType()
        );
        assertEquals(
            EventSeverity.WARNING,
            response.severity()
        );
        assertEquals(
            "kubernetes",
            response.source()
        );
        assertEquals(
            "The workload pod restarted.",
            response.message()
        );
        assertEquals(
            occurredAt,
            response.occurredAt()
        );
    }
}
