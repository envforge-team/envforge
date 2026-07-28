package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.EnvironmentLifecycleContext;
import com.envforge.cleanupworker.domain.EnvironmentStatus;
import org.springframework.stereotype.Service;

@Service
public class LifecycleDecisionService {

    private final LifecycleTransitionValidator transitionValidator;

    public LifecycleDecisionService(LifecycleTransitionValidator transitionValidator) {
        this.transitionValidator = transitionValidator;
    }

    public EnvironmentStatus requestDelete(EnvironmentLifecycleContext context) {
        transitionValidator.validate(context.status(), EnvironmentStatus.DELETING);
        return EnvironmentStatus.DELETING;
    }

    public EnvironmentStatus requestExpiration(EnvironmentLifecycleContext context) {
        transitionValidator.validate(context.status(), EnvironmentStatus.EXPIRED);
        return EnvironmentStatus.EXPIRED;
    }

    public EnvironmentStatus requestRollback(EnvironmentLifecycleContext context) {
        if (context.previousSuccessfulRevision() == null) {
            throw new InvalidLifecycleTransitionException(
                    "Rollback requires a previous successful Helm revision"
            );
        }

        transitionValidator.validate(context.status(), EnvironmentStatus.ROLLING_BACK);
        return EnvironmentStatus.ROLLING_BACK;
    }

    public EnvironmentStatus markDeleteSucceeded(EnvironmentLifecycleContext context) {
        transitionValidator.validate(context.status(), EnvironmentStatus.DELETED);
        return EnvironmentStatus.DELETED;
    }

    public EnvironmentStatus markDeleteFailed(EnvironmentLifecycleContext context) {
        transitionValidator.validate(context.status(), EnvironmentStatus.DELETE_FAILED);
        return EnvironmentStatus.DELETE_FAILED;
    }

    public EnvironmentStatus markRollbackSucceeded(EnvironmentLifecycleContext context) {
        transitionValidator.validate(context.status(), EnvironmentStatus.READY);
        return EnvironmentStatus.READY;
    }

    public EnvironmentStatus markRollbackFailed(EnvironmentLifecycleContext context) {
        transitionValidator.validate(context.status(), EnvironmentStatus.ROLLBACK_FAILED);
        return EnvironmentStatus.ROLLBACK_FAILED;
    }
}
