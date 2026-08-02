package com.envforge.controlapi.deployment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<DeploymentEntity, Long> {
    List<DeploymentEntity> findByEnvironmentIdOrderByStartedAtDesc(UUID environmentId);
}