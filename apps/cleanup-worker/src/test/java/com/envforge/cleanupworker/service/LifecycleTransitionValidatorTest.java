package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.EnvironmentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleTransitionValidatorTest {

    private final LifecycleTransitionValidator validator =
            new LifecycleTransitionValidator();

    @Test
    void shouldAllowReadyToDeleting() {
        assertTrue(
                validator.isAllowed(
                        EnvironmentStatus.READY,
                        EnvironmentStatus.DELETING
                )
        );
    }

    @Test
    void shouldAllowDeletingToDeleted() {
        assertTrue(
                validator.isAllowed(
                        EnvironmentStatus.DELETING,
                        EnvironmentStatus.DELETED
                )
        );
    }

    @Test
    void shouldRejectDeletedToReady() {
        assertFalse(
                validator.isAllowed(
                        EnvironmentStatus.DELETED,
                        EnvironmentStatus.READY
                )
        );
    }

    @Test
    void shouldThrowForInvalidTransition() {
        assertThrows(
                InvalidLifecycleTransitionException.class,
                () -> validator.validate(
                        EnvironmentStatus.DELETED,
                        EnvironmentStatus.READY
                )
        );
    }
}
