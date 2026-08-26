package com.envforge.controlapi.runtime;

import java.time.Instant;
import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentRuntimeService {

    private final EnvironmentRepository
        environmentRepository;

    private final EnvironmentRuntimeInspector
        runtimeInspector;

    public EnvironmentRuntimeService(
        EnvironmentRepository environmentRepository,
        EnvironmentRuntimeInspector runtimeInspector
    ) {
        this.environmentRepository =
            environmentRepository;

        this.runtimeInspector = runtimeInspector;
    }

    @Transactional(readOnly = true)
    public EnvironmentRuntimeResponse inspect(
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

        EnvironmentRuntimeSnapshot snapshot =
            runtimeInspector.inspect(
                environment.getNamespace(),
                environment.getName()
            );

        return new EnvironmentRuntimeResponse(
            environment.getId(),
            environment.getName(),
            environment.getNamespace(),
            snapshot.namespaceExists(),
            environment.getName(),
            snapshot.helmStatus(),
            snapshot.deploymentName(),
            snapshot.desiredReplicas(),
            snapshot.readyReplicas(),
            snapshot.serviceName(),
            Instant.now()
        );
    }
}