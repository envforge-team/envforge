package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @InjectMocks
    private DeploymentService deploymentService;

    private EnvironmentEntity environment;
    private UUID environmentId;

    @BeforeEach
    void setUp() {
        environmentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-29T10:00:00Z");

        environment = new EnvironmentEntity(
            environmentId,
            "staging-api",
            "env-staging-api",
            EnvironmentTemplate.STATIC_WEB,
            "0.1.0",
            2,
            ResourceProfile.SMALL,
            EnvironmentStatus.REQUESTED,
            true,
            "test-user",
            now,
            now.plus(4, ChronoUnit.HOURS),
            now
        );
    }

    @Test
    void triggerUpdate_withValidVersion_createsDeployment() {
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));
        when(deploymentRepository.findByEnvironmentIdOrderByStartedAtDesc(environmentId)).thenReturn(List.of());
        when(deploymentRepository.save(any(DeploymentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEnvironmentRequest request = new UpdateEnvironmentRequest("1.4.0");
        DeploymentResponse response = deploymentService.triggerUpdate(environmentId, request);

        assertThat(response.requestedVersion()).isEqualTo("1.4.0");
        assertThat(response.status()).isEqualTo(DeploymentStatus.IN_PROGRESS);
    }

    @Test
    void triggerUpdate_withNonExistentEnvironment_throwsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(environmentRepository.findById(missingId)).thenReturn(Optional.empty());

        UpdateEnvironmentRequest request = new UpdateEnvironmentRequest("1.4.0");

        assertThatThrownBy(() -> deploymentService.triggerUpdate(missingId, request))
            .isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void triggerUpdate_withInvalidVersion_throwsInvalidVersion() {
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));
        when(deploymentRepository.findByEnvironmentIdOrderByStartedAtDesc(environmentId)).thenReturn(List.of());

        UpdateEnvironmentRequest request = new UpdateEnvironmentRequest("not-a-version");

        assertThatThrownBy(() -> deploymentService.triggerUpdate(environmentId, request))
            .isInstanceOf(InvalidVersionException.class);
    }

    @Test
    void triggerUpdate_withActiveRollout_throwsConcurrentRollout() {
        DeploymentEntity activeDeployment = new DeploymentEntity();
        activeDeployment.setStatus(DeploymentStatus.IN_PROGRESS);

        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));
        when(deploymentRepository.findByEnvironmentIdOrderByStartedAtDesc(environmentId))
            .thenReturn(List.of(activeDeployment));

        UpdateEnvironmentRequest request = new UpdateEnvironmentRequest("1.5.0");

        assertThatThrownBy(() -> deploymentService.triggerUpdate(environmentId, request))
            .isInstanceOf(ConcurrentRolloutException.class);
    }
}
