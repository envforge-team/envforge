package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final EnvironmentRepository environmentRepository;

    public DeploymentService(DeploymentRepository deploymentRepository,
                              EnvironmentRepository environmentRepository) {
        this.deploymentRepository = deploymentRepository;
        this.environmentRepository = environmentRepository;
    }

    @Transactional
    public DeploymentResponse triggerUpdate(UUID environmentId, UpdateEnvironmentRequest request) {
        EnvironmentEntity environment = environmentRepository.findById(environmentId)
            .orElseThrow(() -> new EnvironmentNotFoundException(environmentId));

        boolean hasActiveRollout = deploymentRepository
            .findByEnvironmentIdOrderByStartedAtDesc(environmentId).stream()
            .anyMatch(d -> d.getStatus() == DeploymentStatus.PENDING
                        || d.getStatus() == DeploymentStatus.IN_PROGRESS);

        if (hasActiveRollout) {
            throw new ConcurrentRolloutException(environmentId);
        }

        validateVersion(request.version());

        DeploymentEntity deployment = new DeploymentEntity();
        deployment.setEnvironment(environment);
        deployment.setRequestedVersion(request.version());
        deployment.setImageTag(buildImageTag(environment, request.version()));
        deployment.setStatus(DeploymentStatus.PENDING);
        deployment.setTriggeredBy("raoul");
        deployment.setStartedAt(Instant.now());

        deploymentRepository.save(deployment);

        deployment.setStatus(DeploymentStatus.IN_PROGRESS);
        deploymentRepository.save(deployment);

        return DeploymentResponse.fromEntity(deployment);
    }

    public List<DeploymentResponse> getHistory(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new EnvironmentNotFoundException(environmentId);
        }
        return deploymentRepository.findByEnvironmentIdOrderByStartedAtDesc(environmentId).stream()
            .map(DeploymentResponse::fromEntity)
            .toList();
    }

    private void validateVersion(String version) {
        if (version == null || !version.matches("^\\d+\\.\\d+\\.\\d+$")) {
            throw new InvalidVersionException(version);
        }
    }

    private String buildImageTag(EnvironmentEntity environment, String version) {
        return "acr.io/" + environment.getName() + ":" + version;
    }
}