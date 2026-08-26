package com.envforge.controlapi.provisioning;

import java.time.Instant;
import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentResponse;
import com.envforge.controlapi.environment.EnvironmentStatus;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentRetryService {

    private final EnvironmentRepository
        environmentRepository;

    private final ApplicationEventPublisher
        eventPublisher;

    public EnvironmentRetryService(
        EnvironmentRepository environmentRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.environmentRepository =
            environmentRepository;

        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EnvironmentResponse retry(
        UUID environmentId
    ) {
        EnvironmentEntity environment =
            environmentRepository
                .findById(environmentId)
                .orElseThrow(
                    () -> new EnvironmentNotFoundException(
                        environmentId
                    )
                );

        if (
            environment.getStatus()
                != EnvironmentStatus.FAILED
        ) {
            throw new EnvironmentRetryNotAllowedException(
                environmentId,
                environment.getStatus()
            );
        }

        environment.changeStatus(
            EnvironmentStatus.REQUESTED,
            Instant.now()
        );

        EnvironmentEntity saved =
            environmentRepository.save(environment);

        eventPublisher.publishEvent(
            new EnvironmentRequestedEvent(
                saved.getId()
            )
        );

        return EnvironmentResponse.from(saved);
    }
}