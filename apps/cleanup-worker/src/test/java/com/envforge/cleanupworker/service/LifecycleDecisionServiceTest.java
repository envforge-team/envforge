package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.EnvironmentLifecycleContext;
import com.envforge.cleanupworker.domain.EnvironmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LifecycleDecisionServiceTest {

    private final LifecycleDecisionService service =
            new LifecycleDecisionService(new LifecycleTransitionValidator());

    @Test
    void shouldAcceptRollbackWhenPreviousRevisionExists() {
        EnvironmentLifecycleContext context = context(
                EnvironmentStatus.READY,
                4,
                3
        );

        assertEquals(
                EnvironmentStatus.ROLLING_BACK,
                service.requestRollback(context)
        );
    }

    @Test
    void shouldRejectRollbackWithoutPreviousRevision() {
        EnvironmentLifecycleContext context = context(
                EnvironmentStatus.READY,
                1,
                null
        );

        assertThrows(
                InvalidLifecycleTransitionException.class,
                () -> service.requestRollback(context)
        );
    }

    @Test
    void shouldAcceptManualDeleteFromReady() {
        EnvironmentLifecycleContext context = context(
                EnvironmentStatus.READY,
                2,
                1
        );

        assertEquals(
                EnvironmentStatus.DELETING,
                service.requestDelete(context)
        );
    }

    private EnvironmentLifecycleContext context(
            EnvironmentStatus status,
            Integer currentRevision,
            Integer previousRevision
    ) {
        return new EnvironmentLifecycleContext(
                UUID.randomUUID(),
                status,
                "env-test",
                "env-test-release",
                currentRevision,
                previousRevision,
                Instant.now().plusSeconds(3600),
                0
        );
    }
}
