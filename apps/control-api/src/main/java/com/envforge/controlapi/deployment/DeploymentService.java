package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final AuthorizationService authorizationService;
    private final DeploymentStateService deploymentStateService;

    @Autowired
    public DeploymentService(
        DeploymentRepository deploymentRepository,
        EnvironmentRepository environmentRepository,
        DeploymentExecutor deploymentExecutor,
        DeploymentMetrics deploymentMetrics,
        CurrentUserProvider currentUserProvider,
        AuthorizationService authorizationService,
        DeploymentStateService deploymentStateService
    ) {
        this.deploymentRepository = deploymentRepository;
        this.environmentRepository = environmentRepository;
        this.deploymentExecutor = deploymentExecutor;
        this.deploymentMetrics = deploymentMetrics;
        this.currentUserProvider = currentUserProvider;
        this.authorizationService = authorizationService;
        this.deploymentStateService =
            deploymentStateService;
    }

    DeploymentService(
        DeploymentRepository deploymentRepository,
        EnvironmentRepository environmentRepository,
        DeploymentExecutor deploymentExecutor,
        DeploymentMetrics deploymentMetrics,
        CurrentUserProvider currentUserProvider,
        AuthorizationService authorizationService
    ) {
        this(
            deploymentRepository,
            environmentRepository,
            deploymentExecutor,
            deploymentMetrics,
            currentUserProvider,
            authorizationService,
            new DeploymentStateService(
                deploymentRepository,
                environmentRepository
            )
        );
    }

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

        CurrentUser currentUser =
            currentUserProvider.getCurrentUser();

        authorizationService.requireOwnerOrAdmin(
            currentUser,
            "UPDATE_ENVIRONMENT",
            environment.getCreatedBy()
        );

        validateVersion(request.version());

        String imageTag =
            buildImageTag(
                environment,
                request.version()
            );

        DeploymentStateService.DeploymentClaim claim =
            deploymentStateService.claim(
                environmentId,
                request.version(),
                imageTag,
                currentUser.email()
            );

        RuntimeException rolloutFailure = null;

        try {
            deploymentExecutor.deploy(
                claim.environment(),
                request.version()
            );
        } catch (RuntimeException exception) {
            rolloutFailure = exception;
        }

        Instant finishedAt = Instant.now();

        DeploymentEntity completedDeployment;

        if (rolloutFailure == null) {
            completedDeployment =
                deploymentStateService
                    .completeSuccess(
                        claim,
                        request.version(),
                        finishedAt
                    );
        } else {
            completedDeployment =
                deploymentStateService
                    .completeFailure(
                        claim,
                        failureReason(
                            rolloutFailure
                        ),
                        finishedAt
                    );
        }

        deploymentMetrics.record(
            completedDeployment.getStatus(),
            claim.startedAt(),
            completedDeployment.getFinishedAt()
        );

        return DeploymentResponse.fromEntity(
            completedDeployment
        );
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> getHistory(
        UUID environmentId
    ) {
        EnvironmentEntity environment =
            environmentRepository.findById(environmentId)
                .orElseThrow(
                    () -> new EnvironmentNotFoundException(
                        environmentId
                    )
                );

        CurrentUser currentUser =
            currentUserProvider.getCurrentUser();

        authorizationService.requireOwnerOrAdmin(
            currentUser,
            "VIEW_DEPLOYMENT_HISTORY",
            environment.getCreatedBy()
        );

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
