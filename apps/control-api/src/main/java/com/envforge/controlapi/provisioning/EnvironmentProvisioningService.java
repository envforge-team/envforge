package com.envforge.controlapi.provisioning;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentProvisioningService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            EnvironmentProvisioningService.class
        );

    private static final String ATTEMPTS_METRIC =
        "envforge.provisioning.attempts";

    private static final String DURATION_METRIC =
        "envforge.provisioning.duration";

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentProvisioner environmentProvisioner;
    private final MeterRegistry meterRegistry;

    public EnvironmentProvisioningService(
        EnvironmentRepository environmentRepository,
        EnvironmentProvisioner environmentProvisioner,
        MeterRegistry meterRegistry
    ) {
        this.environmentRepository = environmentRepository;
        this.environmentProvisioner = environmentProvisioner;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provision(UUID environmentId) {
        EnvironmentEntity environment =
            environmentRepository
                .findById(environmentId)
                .orElseThrow(
                    () -> new EnvironmentNotFoundException(
                        environmentId
                    )
                );

        Timer.Sample timerSample =
            Timer.start(meterRegistry);

        environment.changeStatus(
            EnvironmentStatus.PROVISIONING,
            Instant.now()
        );

        environmentRepository.saveAndFlush(environment);

        String outcome;

        try {
            environmentProvisioner.provision(environment);

            environment.changeStatus(
                EnvironmentStatus.READY,
                Instant.now()
            );

            outcome = "success";

            LOGGER.info(
                "Environment {} is ready",
                environment.getName()
            );
        } catch (RuntimeException exception) {
            environment.changeStatus(
                EnvironmentStatus.FAILED,
                Instant.now()
            );

            outcome = "failure";

            LOGGER.error(
                "Provisioning failed for environment {}",
                environment.getName(),
                exception
            );
        }

        environmentRepository.saveAndFlush(environment);

        recordMetrics(
            timerSample,
            environment,
            outcome
        );
    }

    private void recordMetrics(
        Timer.Sample timerSample,
        EnvironmentEntity environment,
        String outcome
    ) {
        String template = environment
            .getTemplate()
            .name()
            .toLowerCase(Locale.ROOT);

        meterRegistry
            .counter(
                ATTEMPTS_METRIC,
                "outcome",
                outcome,
                "template",
                template
            )
            .increment();

        timerSample.stop(
            Timer.builder(DURATION_METRIC)
                .description(
                    "Time required to provision "
                        + "an EnvForge environment"
                )
                .tag("outcome", outcome)
                .tag("template", template)
                .register(meterRegistry)
        );
    }
}