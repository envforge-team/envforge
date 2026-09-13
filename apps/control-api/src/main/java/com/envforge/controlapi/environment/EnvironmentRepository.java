package com.envforge.controlapi.environment;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnvironmentRepository
    extends JpaRepository<EnvironmentEntity, UUID> {

    Optional<EnvironmentEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNamespace(String namespace);

    List<EnvironmentEntity> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select environment "
            + "from EnvironmentEntity environment "
            + "where environment.id = :id"
    )
    Optional<EnvironmentEntity> findByIdForUpdate(
        @Param("id") UUID id
    );
}
