package com.envforge.controlapi.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    List<AuditEventEntity> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
        String resourceType,
        String resourceId
    );

    List<AuditEventEntity> findByActorOrderByCreatedAtDesc(String actor);
}
