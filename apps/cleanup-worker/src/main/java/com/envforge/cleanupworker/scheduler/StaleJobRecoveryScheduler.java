package com.envforge.cleanupworker.scheduler;

import com.envforge.cleanupworker.config.LifecycleProperties;
import com.envforge.cleanupworker.domain.AuditResult;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import com.envforge.cleanupworker.service.LifecycleAuditService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "envforge.lifecycle",
        name = "scheduler-enabled",
        havingValue = "true"
)
public class StaleJobRecoveryScheduler {

    private final LifecycleJobRepository jobRepository;
    private final LifecycleAuditService auditService;
    private final LifecycleProperties properties;

    public StaleJobRecoveryScheduler(
            LifecycleJobRepository jobRepository,
            LifecycleAuditService auditService,
            LifecycleProperties properties
    ) {
        this.jobRepository = jobRepository;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString =
                    "${envforge.lifecycle.scheduler-delay-milliseconds}"
    )
    @Transactional
    public void recoverStaleJobs() {
        Instant threshold = Instant.now()
                .minusSeconds(properties.jobTimeoutSeconds());

        List<LifecycleJobEntity> staleJobs =
                jobRepository.findByStatusAndStartedAtBefore(
                        LifecycleJobStatus.RUNNING,
                        threshold
                );

        for (LifecycleJobEntity job : staleJobs) {
            String error = "Job exceeded timeout and was recovered";

            if (job.getAttemptCount() < properties.maxRetries()) {
                Instant retryAt = Instant.now()
                        .plusSeconds(properties.retryDelaySeconds());
                job.markRetrying(Instant.now(), retryAt, error);
                auditService.record(job, AuditResult.RETRYING, error);
            } else {
                job.markFailed(Instant.now(), error);
                auditService.record(job, AuditResult.FAILED, error);
            }

            jobRepository.save(job);
        }
    }
}
