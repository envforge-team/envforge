package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleAuditRepository;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LifecycleJobServiceTest {

    @Autowired
    private LifecycleJobService jobService;

    @Autowired
    private LifecycleJobRepository jobRepository;

    @Autowired
    private LifecycleAuditRepository auditRepository;

    @BeforeEach
    void cleanDatabase() {
        auditRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void shouldCreateQueuedJobAndAuditEvent() {
        UUID environmentId = UUID.randomUUID();

        LifecycleJobEntity job = jobService.createJob(
                environmentId,
                LifecycleAction.DELETE,
                null,
                "test-user",
                "env-test",
                "env-test-release"
        );

        assertEquals(LifecycleJobStatus.QUEUED, job.getStatus());
        assertEquals(1, auditRepository.count());
    }

    @Test
    void shouldRejectSecondActiveJobForSameEnvironment() {
        UUID environmentId = UUID.randomUUID();

        jobService.createJob(
                environmentId,
                LifecycleAction.DELETE,
                null,
                "test-user",
                "env-test",
                "env-test-release"
        );

        assertThrows(
                IllegalStateException.class,
                () -> jobService.createJob(
                        environmentId,
                        LifecycleAction.ROLLBACK,
                        2,
                        "test-user",
                        "env-test",
                        "env-test-release"
                )
        );
    }
}
