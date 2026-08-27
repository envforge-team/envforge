package com.envforge.controlapi.provisioning;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;

/**
 * Ziua 36: a missing/invalid Helm chart path (e.g. the packaged jar run
 * from a working directory other than apps/control-api) must not crash
 * bean creation or app startup, and provisioning itself must fail with a
 * clear, actionable message instead of an opaque helm CLI error.
 */
class LocalHelmEnvironmentProvisionerTest {

    @Test
    void constructorDoesNotFailWhenChartPathIsMissing() {
        assertThatCode(() ->
            new LocalHelmEnvironmentProvisioner(
                "kind-envforge",
                "does/not/exist/anywhere"
            )
        ).doesNotThrowAnyException();
    }

    @Test
    void provisionFailsClearlyWhenChartPathIsMissing() {
        LocalHelmEnvironmentProvisioner provisioner =
            new LocalHelmEnvironmentProvisioner(
                "kind-envforge",
                "does/not/exist/anywhere"
            );

        EnvironmentEntity environment = environment();

        assertThatThrownBy(() -> provisioner.provision(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Helm chart path")
            .hasMessageContaining("does not exist");
    }

    private static EnvironmentEntity environment() {
        Instant now = Instant.parse("2026-07-29T10:00:00Z");
        return new EnvironmentEntity(
            UUID.randomUUID(),
            "demo",
            "env-demo",
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
}
