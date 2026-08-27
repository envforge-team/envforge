package com.envforge.cleanupworker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LifecycleStartupDiagnostics implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(LifecycleStartupDiagnostics.class);

    private final LifecycleProperties properties;
    private final Environment environment;

    public LifecycleStartupDiagnostics(
            LifecycleProperties properties,
            Environment environment
    ) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Lifecycle configuration: schedulerEnabled={}, schedulerDelayMilliseconds={}, batchSize={}, maxRetries={}, retryDelaySeconds={}, jobTimeoutSeconds={}",
                properties.schedulerEnabled(),
                properties.schedulerDelayMilliseconds(),
                properties.batchSize(),
                properties.maxRetries(),
                properties.retryDelaySeconds(),
                properties.jobTimeoutSeconds()
        );

        log.info(
                "Lifecycle runner mode={}, kubeContext={}",
                environment.getProperty(
                        "envforge.lifecycle.runner.mode",
                        "dry-run"
                ),
                environment.getProperty(
                        "envforge.lifecycle.kube-context",
                        "kind-envforge"
                )
        );
    }
}
