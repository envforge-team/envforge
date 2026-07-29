package com.envforge.controlapi.environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

final class TestEnvironmentFactory {

    private TestEnvironmentFactory() {
    }

    static EnvironmentEntity environment(
        UUID id,
        String name
    ) {
        Instant now = Instant.parse(
            "2026-07-29T10:00:00Z"
        );

        return new EnvironmentEntity(
            id,
            name,
            "env-" + name,
            EnvironmentTemplate.STATIC_WEB,
            "0.1.0",
            2,
            ResourceProfile.SMALL,
            EnvironmentStatus.REQUESTED,
            true,
            "test-user",
            now,
            now.plus(4, ChronoUnit.HOURS),
            now
        );
    }

    static EnvironmentResponse response(
        UUID id,
        String name
    ) {
        return EnvironmentResponse.from(
            environment(id, name)
        );
    }
}