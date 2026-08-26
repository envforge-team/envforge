package com.envforge.cleanupworker.metrics;

import com.envforge.cleanupworker.domain.LifecycleAction;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LifecycleMetrics {

    private final MeterRegistry meterRegistry;

    public LifecycleMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(
            LifecycleAction action,
            String result,
            Duration duration
    ) {
        String actionTag = action.name().toLowerCase();

        meterRegistry.counter(
                "envforge.lifecycle.operations",
                "action",
                actionTag,
                "result",
                result
        ).increment();

        Timer.builder("envforge.lifecycle.operation.duration")
                .tag("action", actionTag)
                .tag("result", result)
                .register(meterRegistry)
                .record(duration);
    }
}
