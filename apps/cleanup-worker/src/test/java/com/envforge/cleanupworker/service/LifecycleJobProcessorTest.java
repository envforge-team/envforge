package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.config.LifecycleProperties;
import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.environment.EnvironmentEntity;
import com.envforge.cleanupworker.environment.EnvironmentRepository;
import com.envforge.cleanupworker.environment.EnvironmentStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleAuditRepository;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import com.envforge.cleanupworker.runner.CommandResult;
import com.envforge.cleanupworker.runner.LifecycleCommandRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifecycleJobProcessorTest {

    private LifecycleCommandRunner commandRunner;
    private EnvironmentRepository environmentRepository;
    private LifecycleJobProcessor processor;

    @BeforeEach
    void setUp() {
        LifecycleJobRepository jobRepository =
                mock(LifecycleJobRepository.class);
        LifecycleAuditRepository auditRepository =
                mock(LifecycleAuditRepository.class);
        commandRunner = mock(LifecycleCommandRunner.class);
        environmentRepository = mock(EnvironmentRepository.class);

        LifecycleAuditService auditService =
                new LifecycleAuditService(auditRepository);

        LifecycleProperties properties =
                new LifecycleProperties(3, 1, 30, false, 10000, 10);

        processor = new LifecycleJobProcessor(
                jobRepository,
                auditService,
                commandRunner,
                properties,
                environmentRepository
        );
    }

    @Test
    void shouldMarkDeleteJobAndEnvironmentSucceeded() {
        LifecycleJobEntity job = createDeleteJob();
        EnvironmentEntity environment = createEnvironment(job.getEnvironmentId());

        when(environmentRepository.findByIdForUpdate(job.getEnvironmentId()))
                .thenReturn(Optional.of(environment));
        when(commandRunner.uninstall("release", "namespace"))
                .thenReturn(CommandResult.success("uninstalled"));
        when(commandRunner.verifyCleanup("release", "namespace"))
                .thenReturn(CommandResult.success("verified"));

        processor.process(job);

        assertEquals(LifecycleJobStatus.SUCCEEDED, job.getStatus());
        assertEquals(EnvironmentStatus.DELETED, environment.getStatus());
        assertEquals(1, job.getAttemptCount());
    }

    @Test
    void shouldScheduleRetryAfterTemporaryFailure() {
        LifecycleJobEntity job = createDeleteJob();
        EnvironmentEntity environment = createEnvironment(job.getEnvironmentId());

        when(environmentRepository.findByIdForUpdate(job.getEnvironmentId()))
                .thenReturn(Optional.of(environment));
        when(commandRunner.uninstall("release", "namespace"))
                .thenReturn(CommandResult.failure(1, "temporary failure"));

        processor.process(job);

        assertEquals(LifecycleJobStatus.RETRYING, job.getStatus());
        assertEquals(EnvironmentStatus.DELETING, environment.getStatus());
        assertEquals(1, job.getAttemptCount());
    }

    private LifecycleJobEntity createDeleteJob() {
        Instant now = Instant.now();

        return new LifecycleJobEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LifecycleAction.DELETE,
                LifecycleJobStatus.QUEUED,
                0,
                null,
                "test-user",
                "namespace",
                "release",
                now,
                now
        );
    }

    private EnvironmentEntity createEnvironment(UUID id) {
        Instant now = Instant.now();

        return new EnvironmentEntity(
                id,
                "release",
                "namespace",
                EnvironmentStatus.READY,
                now.minusSeconds(60),
                now
        );
    }
}
