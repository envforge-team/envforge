package com.envforge.controlapi.monitoring;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentEventRepository
    extends JpaRepository<EnvironmentEventEntity, UUID> {

    List<EnvironmentEventEntity>
        findByEnvironmentIdOrderByOccurredAtDesc(UUID environmentId);

    List<EnvironmentEventEntity>
        findAllByOrderByOccurredAtDesc();
}
