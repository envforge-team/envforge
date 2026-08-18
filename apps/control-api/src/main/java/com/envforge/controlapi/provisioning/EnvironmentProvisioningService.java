package com.envforge.controlapi.provisioning;

import java.time.Instant;
import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;

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

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentProvisioner environmentProvisioner;

    public EnvironmentProvisioningService(
        EnvironmentRepository environmentRepository,
        EnvironmentProvisioner environmentProvisioner
    ) {
        this.environmentRepository = environmentRepository;
        this.environmentProvisioner = environmentProvisioner;
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

        environment.changeStatus(
            EnvironmentStatus.PROVISIONING,
            Instant.now()
        );

        environmentRepository.saveAndFlush(environment);

        try {
            environmentProvisioner.provision(environment);

            environment.changeStatus(
                EnvironmentStatus.READY,
                Instant.now()
            );

            LOGGER.info(
                "Environment {} is ready",
                environment.getName()
            );
        } catch (RuntimeException exception) {
            environment.changeStatus(
                EnvironmentStatus.FAILED,
                Instant.now()
            );

            LOGGER.error(
                "Provisioning failed for environment {}",
                environment.getName(),
                exception
            );
        }

        environmentRepository.saveAndFlush(environment);
    }
}