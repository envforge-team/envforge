package com.envforge.cleanupworker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.environment.EnvironmentEntity;
import com.envforge.cleanupworker.environment.EnvironmentRepository;
import com.envforge.cleanupworker.environment.EnvironmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class EnvironmentExpirationServiceTest {

    @Test
    void shouldQueueExpirationAsSystemActor() {
        EnvironmentRepository environmentRepository =
            mock(EnvironmentRepository.class);

        LifecycleJobService lifecycleJobService =
            mock(LifecycleJobService.class);

        EnvironmentExpirationService service =
            new EnvironmentExpirationService(
                environmentRepository,
                lifecycleJobService
            );

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        EnvironmentEntity environment =
            new EnvironmentEntity(
                id,
                "expired-demo",
                "env-expired-demo",
                EnvironmentStatus.READY,
                now.minusSeconds(60),
                now.minusSeconds(120)
            );

        when(
            environmentRepository.findExpiredForUpdate(
                any(Instant.class),
                any(),
                any(Pageable.class)
            )
        ).thenReturn(List.of(environment));

        service.createJobsForExpiredEnvironments(10);

        assertThat(environment.getStatus())
            .isEqualTo(EnvironmentStatus.EXPIRED);

        verify(lifecycleJobService).createJob(
            eq(id),
            eq(LifecycleAction.EXPIRE),
            eq(null),
            eq("SYSTEM"),
            eq("env-expired-demo"),
            eq("expired-demo")
        );
    }
}
