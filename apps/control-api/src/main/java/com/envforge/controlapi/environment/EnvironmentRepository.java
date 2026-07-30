package com.envforge.controlapi.environment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository
    extends JpaRepository<EnvironmentEntity, UUID> {

    Optional<EnvironmentEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNamespace(String namespace);

    List<EnvironmentEntity> findAllByOrderByCreatedAtDesc();
}