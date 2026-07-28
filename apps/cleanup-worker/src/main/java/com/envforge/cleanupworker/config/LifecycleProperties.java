package com.envforge.cleanupworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "envforge.lifecycle")
public record LifecycleProperties(
        int maxRetries,
        int retryDelaySeconds,
        int jobTimeoutSeconds,
        boolean schedulerEnabled
) {
}
