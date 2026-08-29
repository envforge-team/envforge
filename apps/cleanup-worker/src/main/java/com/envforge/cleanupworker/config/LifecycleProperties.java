package com.envforge.cleanupworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "envforge.lifecycle")
public record LifecycleProperties(
        int maxRetries,
        int retryDelaySeconds,
        int jobTimeoutSeconds,
        boolean schedulerEnabled,
        long schedulerDelayMilliseconds,
        int batchSize
) {
    public LifecycleProperties {
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be at least 1");
        }
        if (retryDelaySeconds < 0) {
            throw new IllegalArgumentException("retryDelaySeconds must not be negative");
        }
        if (jobTimeoutSeconds < 1) {
            throw new IllegalArgumentException("jobTimeoutSeconds must be at least 1");
        }
        if (schedulerDelayMilliseconds < 1000) {
            throw new IllegalArgumentException(
                    "schedulerDelayMilliseconds must be at least 1000"
            );
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
    }
}
