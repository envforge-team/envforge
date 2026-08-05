package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.AuditResult;
import com.envforge.cleanupworker.persistence.entity.LifecycleAuditEntity;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleAuditRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LifecycleAuditService {

    private final LifecycleAuditRepository auditRepository;

    public LifecycleAuditService(LifecycleAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void record(
            LifecycleJobEntity job,
            AuditResult result,
            String details
    ) {
        auditRepository.save(
                new LifecycleAuditEntity(
                        UUID.randomUUID(),
                        job.getEnvironmentId(),
                        job.getId(),
                        job.getActorId(),
                        job.getAction(),
                        result,
                        details,
                        Instant.now()
                )
        );
    }
}
