package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeploymentStateService {

    private final DeploymentRepository deploymentRepository;
    private final EnvironmentRepository environmentRepository;

    public DeploymentStateService(
        DeploymentRepository deploymentRepository,
        EnvironmentRepository environmentRepository
    ) {
        this.deploymentRepository = deploymentRepository;
        this.environmentRepository = environmentRepository;
    }

    @Transactional
    public DeploymentClaim claim(
        UUID environmentId,
        String requestedVersion,
        String imageTag,
        String triggeredBy
    ) {
        EnvironmentEntity environment =
            environmentRepository
                .findByIdForUpdate(environmentId)
                .orElseThrow(
                    () -> new EnvironmentNotFoundException(
                        environmentId
                    )
                );

        boolean hasActiveRollout =
            deploymentRepository
                .findByEnvironmentIdOrderByStartedAtDesc(
                    environmentId
                )
                .stream()
                .anyMatch(
                    deployment ->
                        deployment.getStatus()
                            == DeploymentStatus.PENDING
                        || deployment.getStatus()
                            == DeploymentStatus.IN_PROGRESS
                );

        if (hasActiveRollout) {
            throw new ConcurrentRolloutException(
                environmentId
            );
        }

        Instant startedAt = Instant.now();

        DeploymentEntity deployment =
            new DeploymentEntity();

        deployment.setEnvironment(environment);
        deployment.setRequestedVersion(
            requestedVersion
        );
        deployment.setImageTag(imageTag);
        deployment.setStatus(
            DeploymentStatus.IN_PROGRESS
        );
        deployment.setTriggeredBy(triggeredBy);
        deployment.setStartedAt(startedAt);

        deploymentRepository.save(deployment);

        environment.changeStatus(
            EnvironmentStatus.DEPLOYING,
            startedAt
        );

        environmentRepository.save(environment);

        return new DeploymentClaim(
            deployment,
            environment,
            startedAt
        );
    }

    @Transactional
    public DeploymentEntity completeSuccess(
        DeploymentClaim claim,
        String version,
        Instant finishedAt
    ) {
        DeploymentEntity deployment =
            claim.deployment();

        EnvironmentEntity environment =
            claim.environment();

        deployment.setStatus(
            DeploymentStatus.SUCCESS
        );
        deployment.setFinishedAt(finishedAt);
        deployment.setFailureReason(null);

        environment.changeImageVersion(
            version,
            finishedAt
        );

        environment.changeStatus(
            EnvironmentStatus.READY,
            finishedAt
        );

        environmentRepository.save(environment);

        deploymentRepository.save(
            deployment
        );

        return deployment;
    }

    @Transactional
    public DeploymentEntity completeFailure(
        DeploymentClaim claim,
        String failureReason,
        Instant finishedAt
    ) {
        DeploymentEntity deployment =
            claim.deployment();

        EnvironmentEntity environment =
            claim.environment();

        deployment.setStatus(
            DeploymentStatus.FAILED
        );
        deployment.setFinishedAt(finishedAt);
        deployment.setFailureReason(
            failureReason
        );

        environment.changeStatus(
            EnvironmentStatus.DEGRADED,
            finishedAt
        );

        environmentRepository.save(environment);

        deploymentRepository.save(
            deployment
        );

        return deployment;
    }

    public record DeploymentClaim(
        DeploymentEntity deployment,
        EnvironmentEntity environment,
        Instant startedAt
    ) {
    }
}
