package com.envforge.cleanupworker.scheduler;

import com.envforge.cleanupworker.config.LifecycleProperties;
import com.envforge.cleanupworker.service.EnvironmentExpirationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "envforge.lifecycle",
        name = "scheduler-enabled",
        havingValue = "true"
)
public class EnvironmentExpirationScheduler {

    private final EnvironmentExpirationService expirationService;
    private final LifecycleProperties properties;

    public EnvironmentExpirationScheduler(
            EnvironmentExpirationService expirationService,
            LifecycleProperties properties
    ) {
        this.expirationService = expirationService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString =
                    "${envforge.lifecycle.scheduler-delay-milliseconds}"
    )
    public void expireEnvironments() {
        expirationService.createJobsForExpiredEnvironments(
                properties.batchSize()
        );
    }
}
