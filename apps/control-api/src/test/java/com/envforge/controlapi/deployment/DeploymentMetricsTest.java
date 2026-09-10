package com.envforge.controlapi.deployment;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentMetricsTest {

    @Test
    void recordSuccess_exposesCounterAndDuration() {
        SimpleMeterRegistry registry =
            new SimpleMeterRegistry();

        DeploymentMetrics metrics =
            new DeploymentMetrics(registry);

        Instant startedAt =
            Instant.parse(
                "2026-09-10T10:00:00Z"
            );

        metrics.record(
            DeploymentStatus.SUCCESS,
            startedAt,
            startedAt.plusMillis(5250)
        );

        double count = registry
            .get(DeploymentMetrics.RESULTS_METRIC)
            .tag("result", "success")
            .counter()
            .count();

        Timer timer = registry
            .get(DeploymentMetrics.DURATION_METRIC)
            .tag("result", "success")
            .timer();

        assertThat(count).isEqualTo(1.0);
        assertThat(timer.count()).isEqualTo(1);
        assertThat(
            timer.totalTime(TimeUnit.MILLISECONDS)
        ).isEqualTo(5250.0);
    }

    @Test
    void recordFailure_exposesCounterAndDuration() {
        SimpleMeterRegistry registry =
            new SimpleMeterRegistry();

        DeploymentMetrics metrics =
            new DeploymentMetrics(registry);

        Instant startedAt =
            Instant.parse(
                "2026-09-10T10:00:00Z"
            );

        metrics.record(
            DeploymentStatus.FAILED,
            startedAt,
            startedAt.plusSeconds(2)
        );

        double count = registry
            .get(DeploymentMetrics.RESULTS_METRIC)
            .tag("result", "failed")
            .counter()
            .count();

        Timer timer = registry
            .get(DeploymentMetrics.DURATION_METRIC)
            .tag("result", "failed")
            .timer();

        assertThat(count).isEqualTo(1.0);
        assertThat(timer.count()).isEqualTo(1);
        assertThat(
            timer.totalTime(TimeUnit.MILLISECONDS)
        ).isEqualTo(2000.0);
    }
}
