package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.AuditResult;
import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class LifecycleJobService {

    private static final EnumSet<LifecycleJobStatus> ACTIVE_STATUSES =
            EnumSet.of(
                    LifecycleJobStatus.QUEUED,
                    LifecycleJobStatus.RUNNING,
                    LifecycleJobStatus.RETRYING
            );

    private final LifecycleJobRepository jobRepository;
    private final LifecycleAuditService auditService;

    public LifecycleJobService(
            LifecycleJobRepository jobRepository,
            LifecycleAuditService auditService
    ) {
        this.jobRepository = jobRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LifecycleJobEntity createJob(
            UUID environmentId,
            LifecycleAction action,
            Integer targetRevision,
            String actorId,
            String namespaceName,
            String helmReleaseName
    ) {
        if (jobRepository.existsByEnvironmentIdAndStatusIn(
                environmentId,
                ACTIVE_STATUSES
        )) {
            throw new IllegalStateException(
                    "An active lifecycle job already exists for environment "
                            + environmentId
            );
        }

        Instant now = Instant.now();

        LifecycleJobEntity saved = jobRepository.save(
                new LifecycleJobEntity(
                        UUID.randomUUID(),
                        environmentId,
                        action,
                        LifecycleJobStatus.QUEUED,
                        0,
                        targetRevision,
                        actorId,
                        namespaceName,
                        helmReleaseName,
                        now,
                        now
                )
        );

        auditService.record(saved, AuditResult.REQUESTED, "Lifecycle job queued");
        return saved;
    }
}
