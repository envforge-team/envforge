package com.envforge.cleanupworker.persistence;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.persistence.repository.LifecycleJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class LifecycleJobRepositoryTest {

    @Autowired
    private LifecycleJobRepository repository;

    @Test
    void shouldDetectActiveJobForEnvironment() {
        UUID environmentId = UUID.randomUUID();
        Instant now = Instant.now();

        repository.save(
                new LifecycleJobEntity(
                        UUID.randomUUID(),
                        environmentId,
                        LifecycleAction.DELETE,
                        LifecycleJobStatus.QUEUED,
                        0,
                        null,
                        "test-user",
                        "env-test",
                        "env-test-release",
                        now,
                        now
                )
        );

        boolean exists =
                repository.existsByEnvironmentIdAndStatusIn(
                        environmentId,
                        EnumSet.of(
                                LifecycleJobStatus.QUEUED,
                                LifecycleJobStatus.RUNNING,
                                LifecycleJobStatus.RETRYING
                        )
                );

        assertTrue(exists);
    }
}
