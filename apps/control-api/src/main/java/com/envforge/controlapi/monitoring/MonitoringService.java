package com.envforge.controlapi.monitoring;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;

@Service
public class MonitoringService {

    private final EnvironmentRepository environmentRepository;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final EnvironmentEventRepository environmentEventRepository;

    public MonitoringService(
        EnvironmentRepository environmentRepository,
        HealthSnapshotRepository healthSnapshotRepository,
        EnvironmentEventRepository environmentEventRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.healthSnapshotRepository = healthSnapshotRepository;
        this.environmentEventRepository = environmentEventRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MetricResponse> findLatestMetrics(
        UUID environmentId
    ) {
        EnvironmentEntity environment =
            requireEnvironment(environmentId);

        return healthSnapshotRepository
            .findTopByEnvironmentIdOrderByCapturedAtDesc(
                environmentId
            )
            .map(snapshot ->
                MetricResponse.from(environment, snapshot)
            );
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findEvents(
        UUID environmentId
    ) {
        requireEnvironment(environmentId);

        return environmentEventRepository
            .findByEnvironmentIdOrderByOccurredAtDesc(
                environmentId
            )
            .stream()
            .map(EventResponse::from)
            .toList();
    }

    private EnvironmentEntity requireEnvironment(
        UUID environmentId
    ) {
        return environmentRepository
            .findById(environmentId)
            .orElseThrow(
                () -> new EnvironmentNotFoundException(
                    environmentId
                )
            );
    }
}
