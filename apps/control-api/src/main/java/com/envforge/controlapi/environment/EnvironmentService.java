package com.envforge.controlapi.environment;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final Clock clock;

    public EnvironmentService(
        EnvironmentRepository environmentRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public EnvironmentResponse create(
        CreateEnvironmentRequest request
    ) {
        if (environmentRepository.existsByName(request.name())) {
            throw new EnvironmentAlreadyExistsException(
                request.name()
            );
        }

        String namespace = "env-" + request.name();

        if (environmentRepository.existsByNamespace(namespace)) {
            throw new EnvironmentAlreadyExistsException(
                request.name()
            );
        }

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(
            request.lifetimeHours(),
            ChronoUnit.HOURS
        );

        EnvironmentEntity environment = new EnvironmentEntity(
            UUID.randomUUID(),
            request.name(),
            namespace,
            request.template(),
            request.imageVersion(),
            request.replicas(),
            request.resourceProfile(),
            EnvironmentStatus.REQUESTED,
            request.monitoringEnabled(),
            "local-user",
            now,
            expiresAt,
            now
        );

        EnvironmentEntity saved =
            environmentRepository.save(environment);

        return EnvironmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> findAll() {
        return environmentRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(EnvironmentResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse findById(UUID id) {
        return environmentRepository
            .findById(id)
            .map(EnvironmentResponse::from)
            .orElseThrow(
                () -> new EnvironmentNotFoundException(id)
            );
    }



}