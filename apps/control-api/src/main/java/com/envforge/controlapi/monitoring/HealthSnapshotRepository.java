package com.envforge.controlapi.monitoring;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthSnapshotRepository
    extends JpaRepository<HealthSnapshotEntity, UUID> {

    Optional<HealthSnapshotEntity>
        findTopByEnvironmentIdOrderByCapturedAtDesc(UUID environmentId);

    List<HealthSnapshotEntity>
        findByEnvironmentIdOrderByCapturedAtDesc(UUID environmentId);
}
