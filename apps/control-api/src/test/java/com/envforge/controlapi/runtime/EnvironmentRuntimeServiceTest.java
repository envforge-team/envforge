package com.envforge.controlapi.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentRuntimeServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private EnvironmentRuntimeInspector runtimeInspector;

    private EnvironmentRuntimeService runtimeService;

    @BeforeEach
    void setUp() {
        runtimeService = new EnvironmentRuntimeService(
            environmentRepository,
            runtimeInspector
        );
    }

    @Test
    void shouldReturnEnvironmentRuntimeDetails() {
        EnvironmentEntity environment =
            createEnvironment();

        EnvironmentRuntimeSnapshot snapshot =
            new EnvironmentRuntimeSnapshot(
                true,
                "deployed",
                "runtime-demo-envforge-workload",
                1,
                1,
                "runtime-demo-envforge-workload"
            );

        when(
            environmentRepository.findById(
                environment.getId()
            )
        ).thenReturn(Optional.of(environment));

        when(
            runtimeInspector.inspect(
                environment.getNamespace(),
                environment.getName()
            )
        ).thenReturn(snapshot);

        EnvironmentRuntimeResponse response =
            runtimeService.inspect(
                environment.getId()
            );

        assertThat(response.environmentId())
            .isEqualTo(environment.getId());

        assertThat(response.environmentName())
            .isEqualTo("runtime-demo");

        assertThat(response.namespace())
            .isEqualTo("env-runtime-demo");

        assertThat(response.namespaceExists())
            .isTrue();

        assertThat(response.helmRelease())
            .isEqualTo("runtime-demo");

        assertThat(response.helmStatus())
            .isEqualTo("deployed");

        assertThat(response.deploymentName())
            .isEqualTo(
                "runtime-demo-envforge-workload"
            );

        assertThat(response.desiredReplicas())
            .isEqualTo(1);

        assertThat(response.readyReplicas())
            .isEqualTo(1);

        assertThat(response.serviceName())
            .isEqualTo(
                "runtime-demo-envforge-workload"
            );

        assertThat(response.observedAt())
            .isNotNull();

        verify(runtimeInspector).inspect(
            "env-runtime-demo",
            "runtime-demo"
        );
    }

    @Test
    void shouldReturnMissingKubernetesResources() {
        EnvironmentEntity environment =
            createEnvironment();

        EnvironmentRuntimeSnapshot snapshot =
            new EnvironmentRuntimeSnapshot(
                false,
                "not-found",
                null,
                null,
                null,
                null
            );

        when(
            environmentRepository.findById(
                environment.getId()
            )
        ).thenReturn(Optional.of(environment));

        when(
            runtimeInspector.inspect(
                environment.getNamespace(),
                environment.getName()
            )
        ).thenReturn(snapshot);

        EnvironmentRuntimeResponse response =
            runtimeService.inspect(
                environment.getId()
            );

        assertThat(response.namespaceExists())
            .isFalse();

        assertThat(response.helmStatus())
            .isEqualTo("not-found");

        assertThat(response.deploymentName())
            .isNull();

        assertThat(response.desiredReplicas())
            .isNull();

        assertThat(response.readyReplicas())
            .isNull();

        assertThat(response.serviceName())
            .isNull();
    }

    @Test
    void shouldRejectUnknownEnvironment() {
        UUID environmentId = UUID.randomUUID();

        when(
            environmentRepository.findById(
                environmentId
            )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> runtimeService.inspect(
                environmentId
            )
        )
            .isInstanceOf(
                EnvironmentNotFoundException.class
            )
            .hasMessage(
                "Environment not found: "
                    + environmentId
            );

        verifyNoInteractions(runtimeInspector);
    }

    private EnvironmentEntity createEnvironment() {
        Instant now =
            Instant.parse("2026-08-26T10:00:00Z");

        return new EnvironmentEntity(
            UUID.randomUUID(),
            "runtime-demo",
            "env-runtime-demo",
            EnvironmentTemplate.STATIC_WEB,
            "0.2.0",
            1,
            ResourceProfile.SMALL,
            EnvironmentStatus.READY,
            true,
            "test-user",
            now,
            now.plus(2, ChronoUnit.HOURS),
            now
        );
    }
}