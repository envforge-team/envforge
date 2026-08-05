package com.envforge.cleanupworker.persistence.repository;

import com.envforge.cleanupworker.persistence.entity.LifecycleAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LifecycleAuditRepository
        extends JpaRepository<LifecycleAuditEntity, UUID> {

    List<LifecycleAuditEntity> findByEnvironmentIdOrderByCreatedAtAsc(
            UUID environmentId
    );
}
