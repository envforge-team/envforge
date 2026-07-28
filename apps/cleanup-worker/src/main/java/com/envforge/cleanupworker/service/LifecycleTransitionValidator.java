package com.envforge.cleanupworker.service;

import com.envforge.cleanupworker.domain.EnvironmentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class LifecycleTransitionValidator {

    private final Map<EnvironmentStatus, Set<EnvironmentStatus>> allowedTransitions;

    public LifecycleTransitionValidator() {
        this.allowedTransitions = new EnumMap<>(EnvironmentStatus.class);

        allow(EnvironmentStatus.PROVISIONING,
                EnvironmentStatus.READY,
                EnvironmentStatus.DELETE_FAILED);

        allow(EnvironmentStatus.READY,
                EnvironmentStatus.UPDATING,
                EnvironmentStatus.ROLLING_BACK,
                EnvironmentStatus.EXPIRED,
                EnvironmentStatus.DELETING);

        allow(EnvironmentStatus.UPDATING,
                EnvironmentStatus.READY,
                EnvironmentStatus.UPDATE_FAILED);

        allow(EnvironmentStatus.UPDATE_FAILED,
                EnvironmentStatus.ROLLING_BACK,
                EnvironmentStatus.EXPIRED,
                EnvironmentStatus.DELETING);

        allow(EnvironmentStatus.ROLLING_BACK,
                EnvironmentStatus.READY,
                EnvironmentStatus.ROLLBACK_FAILED);

        allow(EnvironmentStatus.ROLLBACK_FAILED,
                EnvironmentStatus.ROLLING_BACK,
                EnvironmentStatus.DELETING);

        allow(EnvironmentStatus.EXPIRED,
                EnvironmentStatus.DELETING);

        allow(EnvironmentStatus.DELETING,
                EnvironmentStatus.DELETED,
                EnvironmentStatus.DELETE_FAILED);

        allow(EnvironmentStatus.DELETE_FAILED,
                EnvironmentStatus.DELETING);

        allow(EnvironmentStatus.DELETED);
    }

    public boolean isAllowed(EnvironmentStatus current, EnvironmentStatus next) {
        if (current == null || next == null) {
            return false;
        }

        return allowedTransitions
                .getOrDefault(current, Set.of())
                .contains(next);
    }

    public void validate(EnvironmentStatus current, EnvironmentStatus next) {
        if (!isAllowed(current, next)) {
            throw new InvalidLifecycleTransitionException(
                    "Invalid lifecycle transition: " + current + " -> " + next
            );
        }
    }

    private void allow(EnvironmentStatus current, EnvironmentStatus... nextStatuses) {
        if (nextStatuses.length == 0) {
            allowedTransitions.put(current, EnumSet.noneOf(EnvironmentStatus.class));
            return;
        }

        allowedTransitions.put(
                current,
                EnumSet.of(nextStatuses[0], nextStatuses)
        );
    }
}
