package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.config.LifecycleProperties;
import com.envforge.cleanupworker.domain.AuditResult;
import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.environment.EnvironmentEntity;
import com.envforge.cleanupworker.environment.EnvironmentRepository;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import com.envforge.cleanupworker.runner.CommandResult;
import com.envforge.cleanupworker.runner.LifecycleCommandRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LifecycleJobProcessor {

    private final LifecycleJobRepository jobRepository;
    private final LifecycleAuditService auditService;
    private final LifecycleCommandRunner commandRunner;
    private final LifecycleProperties properties;
    private final EnvironmentRepository environmentRepository;

    public LifecycleJobProcessor(
            LifecycleJobRepository jobRepository,
            LifecycleAuditService auditService,
            LifecycleCommandRunner commandRunner,
            LifecycleProperties properties,
            EnvironmentRepository environmentRepository
    ) {
        this.jobRepository = jobRepository;
        this.auditService = auditService;
        this.commandRunner = commandRunner;
        this.properties = properties;
        this.environmentRepository = environmentRepository;
    }

    @Transactional
    public void process(LifecycleJobEntity job) {
        EnvironmentEntity environment =
                environmentRepository.findByIdForUpdate(job.getEnvironmentId())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Environment not found: "
                                                + job.getEnvironmentId()
                                )
                        );

        if (isDeleteAction(job.getAction())) {
            environment.markDeleting(Instant.now());
            environmentRepository.save(environment);
        }

        job.markRunning(Instant.now());
        jobRepository.save(job);
        auditService.record(job, AuditResult.RUNNING, "Job processing started");

        CommandResult result;

        try {
            result = execute(job);
        } catch (RuntimeException exception) {
            handleFailure(job, environment, exception.getMessage());
            return;
        }

        if (result.successful()) {
            job.markSucceeded(Instant.now());
            jobRepository.save(job);

            if (isDeleteAction(job.getAction())) {
                environment.markDeleted(Instant.now());
            } else if (isRollbackAction(job.getAction())) {
                environment.markReady(Instant.now());
            }

            environmentRepository.save(environment);
            auditService.record(job, AuditResult.SUCCEEDED, result.output());
        } else {
            handleFailure(job, environment, result.error());
        }
    }

    private CommandResult execute(LifecycleJobEntity job) {
        if (job.getHelmReleaseName() == null || job.getNamespaceName() == null) {
            return CommandResult.failure(
                    2,
                    "namespaceName and helmReleaseName are required"
            );
        }

        return switch (job.getAction()) {
            case DELETE, EXPIRE, RETRY_DELETE -> executeDelete(job);
            case ROLLBACK, RETRY_ROLLBACK -> executeRollback(job);
            case EXTEND_LIFETIME -> CommandResult.failure(
                    3,
                    "EXTEND_LIFETIME is not executed by cleanup-worker"
            );
        };
    }

    private CommandResult executeDelete(LifecycleJobEntity job) {
        CommandResult uninstall = commandRunner.uninstall(
                job.getHelmReleaseName(),
                job.getNamespaceName()
        );

        if (!uninstall.successful()) {
            return uninstall;
        }

        return commandRunner.verifyCleanup(
                job.getHelmReleaseName(),
                job.getNamespaceName()
        );
    }

    private CommandResult executeRollback(LifecycleJobEntity job) {
        if (job.getTargetRevision() == null) {
            return CommandResult.failure(
                    4,
                    "Rollback requires targetRevision"
            );
        }

        CommandResult rollback = commandRunner.rollback(
                job.getHelmReleaseName(),
                job.getNamespaceName(),
                job.getTargetRevision()
        );

        if (!rollback.successful()) {
            return rollback;
        }

        return commandRunner.verifyRollback(
                job.getHelmReleaseName(),
                job.getNamespaceName()
        );
    }

    private void handleFailure(
            LifecycleJobEntity job,
            EnvironmentEntity environment,
            String error
    ) {
        Instant now = Instant.now();

        if (job.getAttemptCount() < properties.maxRetries()) {
            Instant retryAt = now.plusSeconds(properties.retryDelaySeconds());
            job.markRetrying(now, retryAt, error);
            jobRepository.save(job);
            auditService.record(job, AuditResult.RETRYING, error);
            return;
        }

        job.markFailed(now, error);
        jobRepository.save(job);
        environment.markFailed(now);
        environmentRepository.save(environment);
        auditService.record(job, AuditResult.FAILED, error);
    }

    private boolean isDeleteAction(LifecycleAction action) {
        return action == LifecycleAction.DELETE
                || action == LifecycleAction.EXPIRE
                || action == LifecycleAction.RETRY_DELETE;
    }

    private boolean isRollbackAction(LifecycleAction action) {
        return action == LifecycleAction.ROLLBACK
                || action == LifecycleAction.RETRY_ROLLBACK;
    }
}
