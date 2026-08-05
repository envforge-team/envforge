package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.environment.EnvironmentEntity;
import com.envforge.cleanupworker.environment.EnvironmentRepository;
import com.envforge.cleanupworker.environment.EnvironmentStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
public class EnvironmentExpirationService {

    private static final EnumSet<EnvironmentStatus> EXPIRABLE_STATUSES =
            EnumSet.of(
                    EnvironmentStatus.READY,
                    EnvironmentStatus.DEGRADED,
                    EnvironmentStatus.FAILED
            );

    private final EnvironmentRepository environmentRepository;
    private final LifecycleJobService lifecycleJobService;

    public EnvironmentExpirationService(
            EnvironmentRepository environmentRepository,
            LifecycleJobService lifecycleJobService
    ) {
        this.environmentRepository = environmentRepository;
        this.lifecycleJobService = lifecycleJobService;
    }

    @Transactional
    public void createJobsForExpiredEnvironments(int batchSize) {
        Instant now = Instant.now();

        List<EnvironmentEntity> environments =
                environmentRepository.findExpiredForUpdate(
                        now,
                        EXPIRABLE_STATUSES,
                        PageRequest.of(0, batchSize)
                );

        for (EnvironmentEntity environment : environments) {
            environment.markExpired(now);
            environmentRepository.save(environment);

            try {
                lifecycleJobService.createJob(
                        environment.getId(),
                        LifecycleAction.EXPIRE,
                        null,
                        "cleanup-worker",
                        environment.getNamespace(),
                        environment.getName()
                );
            } catch (IllegalStateException ignored) {
                // An active lifecycle job already exists.
            }
        }
    }
}
