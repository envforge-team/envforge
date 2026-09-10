package com.envforge.controlapi.deployment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class DeploymentMetrics {

    static final String RESULTS_METRIC =
        "envforge.deployment.results";

    static final String DURATION_METRIC =
        "envforge.deployment.duration";

    private final MeterRegistry meterRegistry;

    public DeploymentMetrics(
        MeterRegistry meterRegistry
    ) {
        this.meterRegistry =
            Objects.requireNonNull(meterRegistry);
    }

    public void record(
        DeploymentStatus status,
        Instant startedAt,
        Instant finishedAt
    ) {
        Objects.requireNonNull(status);
        Objects.requireNonNull(startedAt);
        Objects.requireNonNull(finishedAt);

        String result = switch (status) {
            case SUCCESS -> "success";
            case FAILED -> "failed";
            default -> throw new IllegalArgumentException(
                "Deployment metrics require a final status"
            );
        };

        Duration duration =
            Duration.between(
                startedAt,
                finishedAt
            );

        if (duration.isNegative()) {
            duration = Duration.ZERO;
        }

        Counter.builder(RESULTS_METRIC)
            .description(
                "Number of completed EnvForge deployments"
            )
            .tag("result", result)
            .register(meterRegistry)
            .increment();

        Timer.builder(DURATION_METRIC)
            .description(
                "Duration of completed EnvForge deployments"
            )
            .tag("result", result)
            .register(meterRegistry)
            .record(duration);
    }
}
