package com.envforge.controlapi.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private HealthSnapshotRepository healthSnapshotRepository;

    @Mock
    private EnvironmentEventRepository environmentEventRepository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(
            environmentRepository,
            healthSnapshotRepository,
            environmentEventRepository
        );
    }

    @Test
    void shouldReturnLatestMetrics() {
        UUID environmentId = UUID.randomUUID();
        Instant capturedAt = Instant.parse(
            "2026-07-30T10:40:00Z"
        );

        EnvironmentEntity environment =
            createEnvironment(environmentId);

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

        when(environmentRepository.findById(environmentId))
            .thenReturn(Optional.of(environment));

        when(
            healthSnapshotRepository
                .findTopByEnvironmentIdOrderByCapturedAtDesc(
                    environmentId
                )
        ).thenReturn(Optional.of(snapshot));

        Optional<MetricResponse> result =
            monitoringService.findLatestMetrics(
                environmentId
            );

        assertThat(result).isPresent();

        MetricResponse response = result.orElseThrow();

        assertThat(response.environmentId())
            .isEqualTo(environmentId);
        assertThat(response.environmentName())
            .isEqualTo("monitoring-demo");
        assertThat(response.namespace())
            .isEqualTo("env-monitoring-demo");
        assertThat(response.status())
            .isEqualTo(HealthStatus.HEALTHY);
        assertThat(response.cpuUsagePercent())
            .isEqualTo(23.7);
        assertThat(response.memoryUsageBytes())
            .isEqualTo(384_827_392L);
        assertThat(response.requestRatePerSecond())
            .isEqualTo(18.4);
        assertThat(response.errorRatePercent())
            .isEqualTo(0.6);
        assertThat(response.capturedAt())
            .isEqualTo(capturedAt);
    }

    @Test
    void shouldReturnEmptyWhenNoSnapshotExists() {
        UUID environmentId = UUID.randomUUID();

        when(environmentRepository.findById(environmentId))
            .thenReturn(
                Optional.of(
                    createEnvironment(environmentId)
                )
            );

        when(
            healthSnapshotRepository
                .findTopByEnvironmentIdOrderByCapturedAtDesc(
                    environmentId
                )
        ).thenReturn(Optional.empty());

        Optional<MetricResponse> result =
            monitoringService.findLatestMetrics(
                environmentId
            );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEnvironmentEvents() {
        UUID environmentId = UUID.randomUUID();
        Instant occurredAt = Instant.parse(
            "2026-07-30T10:39:12Z"
        );

        EnvironmentEventEntity event =
            new EnvironmentEventEntity(
                UUID.randomUUID(),
                environmentId,
                EnvironmentEventType.POD_RESTARTED,
                EventSeverity.WARNING,
                "kubernetes",
                "The workload pod restarted.",
                occurredAt
            );

        when(environmentRepository.findById(environmentId))
            .thenReturn(
                Optional.of(
                    createEnvironment(environmentId)
                )
            );

        when(
            environmentEventRepository
                .findByEnvironmentIdOrderByOccurredAtDesc(
                    environmentId
                )
        ).thenReturn(List.of(event));

        List<EventResponse> result =
            monitoringService.findEvents(environmentId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().environmentId())
            .isEqualTo(environmentId);
        assertThat(result.getFirst().eventType())
            .isEqualTo(
                EnvironmentEventType.POD_RESTARTED
            );
        assertThat(result.getFirst().severity())
            .isEqualTo(EventSeverity.WARNING);
        assertThat(result.getFirst().source())
            .isEqualTo("kubernetes");
    }

    @Test
    void shouldThrowWhenEnvironmentDoesNotExist() {
        UUID environmentId = UUID.randomUUID();

        when(environmentRepository.findById(environmentId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> monitoringService.findLatestMetrics(
                environmentId
            )
        )
            .isInstanceOf(
                EnvironmentNotFoundException.class
            )
            .hasMessage(
                "Environment not found: " + environmentId
            );
    }

    private EnvironmentEntity createEnvironment(
        UUID environmentId
    ) {
        Instant now = Instant.parse(
            "2026-07-30T09:00:00Z"
        );

        return new EnvironmentEntity(
            environmentId,
            "monitoring-demo",
            "env-monitoring-demo",
            EnvironmentTemplate.RELIABILITY_API,
            "0.1.0",
            2,
            ResourceProfile.SMALL,
            EnvironmentStatus.READY,
            true,
            "test-user",
            now,
            now.plus(4, ChronoUnit.HOURS),
            now
        );
    }
}
