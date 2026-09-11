package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.security.CurrentUserProvider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final EnvironmentRepository environmentRepository;
    private final DeploymentExecutor deploymentExecutor;
    private final DeploymentMetrics deploymentMetrics;
    private final CurrentUserProvider currentUserProvider;

    public DeploymentService(
        DeploymentRepository deploymentRepository,
        EnvironmentRepository environmentRepository,
        DeploymentExecutor deploymentExecutor,
        DeploymentMetrics deploymentMetrics,
        CurrentUserProvider currentUserProvider
    ) {
        this.deploymentRepository = deploymentRepository;
        this.environmentRepository = environmentRepository;
        this.deploymentExecutor = deploymentExecutor;
        this.deploymentMetrics = deploymentMetrics;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public DeploymentResponse triggerUpdate(
        UUID environmentId,
        UpdateEnvironmentRequest request
    ) {
        EnvironmentEntity environment =
            environmentRepository.findById(environmentId)
                .orElseThrow(
                    () -> new EnvironmentNotFoundException(
                        environmentId
                    )
                );

        boolean hasActiveRollout = deploymentRepository
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

        validateVersion(request.version());

        Instant startedAt = Instant.now();

        DeploymentEntity deployment =
            new DeploymentEntity();

        deployment.setEnvironment(environment);
        deployment.setRequestedVersion(request.version());
        deployment.setImageTag(
            buildImageTag(
                environment,
                request.version()
            )
        );
        deployment.setStatus(DeploymentStatus.PENDING);

        deployment.setTriggeredBy(
            currentUserProvider
                .getCurrentUser()
                .email()
        );

        deployment.setStartedAt(startedAt);

        deploymentRepository.save(deployment);

        deployment.setStatus(
            DeploymentStatus.IN_PROGRESS
        );

        environment.changeStatus(
            EnvironmentStatus.DEPLOYING,
            startedAt
        );

        deploymentRepository.save(deployment);
        environmentRepository.save(environment);

        try {
            deploymentExecutor.deploy(
                environment,
                request.version()
            );

            Instant finishedAt = Instant.now();

            deployment.setStatus(
                DeploymentStatus.SUCCESS
            );
            deployment.setFinishedAt(finishedAt);
            deployment.setFailureReason(null);

            environment.changeImageVersion(
                request.version(),
                finishedAt
            );

            environment.changeStatus(
                EnvironmentStatus.READY,
                finishedAt
            );
        } catch (RuntimeException exception) {
            Instant finishedAt = Instant.now();

            deployment.setStatus(
                DeploymentStatus.FAILED
            );
            deployment.setFinishedAt(finishedAt);
            deployment.setFailureReason(
                failureReason(exception)
            );

            environment.changeStatus(
                EnvironmentStatus.FAILED,
                finishedAt
            );
        }

        deploymentMetrics.record(
            deployment.getStatus(),
            startedAt,
            deployment.getFinishedAt()
        );

        deploymentRepository.save(deployment);
        environmentRepository.save(environment);

        return DeploymentResponse.fromEntity(
            deployment
        );
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> getHistory(
        UUID environmentId
    ) {
        if (!environmentRepository.existsById(
            environmentId
        )) {
            throw new EnvironmentNotFoundException(
                environmentId
            );
        }

        return deploymentRepository
            .findByEnvironmentIdOrderByStartedAtDesc(
                environmentId
            )
            .stream()
            .map(DeploymentResponse::fromEntity)
            .toList();
    }

    private void validateVersion(String version) {
        if (
            version == null
            || !version.matches(
                "^\\d+\\.\\d+\\.\\d+$"
            )
        ) {
            throw new InvalidVersionException(
                version
            );
        }
    }

    private String buildImageTag(
        EnvironmentEntity environment,
        String version
    ) {
        String repository = switch (
            environment.getTemplate()
        ) {
            case STATIC_WEB ->
                "envforge/static-web-demo";
            case RELIABILITY_API ->
                "envforge/reliability-demo-api";
        };

        return repository + ":" + version;
    }

    private String failureReason(
        RuntimeException exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception
                .getClass()
                .getSimpleName();
        }

        if (message.length() <= 1000) {
            return message;
        }

        return message.substring(0, 1000);
    }
}
