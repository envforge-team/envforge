package com.envforge.controlapi.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentProvisioningServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private EnvironmentProvisioner environmentProvisioner;

    private SimpleMeterRegistry meterRegistry;

    private EnvironmentProvisioningService
        environmentProvisioningService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        environmentProvisioningService =
            new EnvironmentProvisioningService(
                environmentRepository,
                environmentProvisioner,
                meterRegistry
            );
    }

    @Test
    void shouldMarkEnvironmentReadyAfterProvisioning() {
        EnvironmentEntity environment =
            createEnvironment("successful-demo");

        configureRepository(environment);

        environmentProvisioningService.provision(
            environment.getId()
        );

        assertThat(environment.getStatus())
            .isEqualTo(EnvironmentStatus.READY);

        verify(environmentProvisioner)
            .provision(environment);

        verify(environmentRepository, times(2))
            .saveAndFlush(environment);

        Counter successCounter = meterRegistry
            .get("envforge.provisioning.attempts")
            .tags(
                "outcome",
                "success",
                "template",
                "static_web"
            )
            .counter();

        assertThat(successCounter.count())
            .isEqualTo(1.0);

        Timer successTimer = meterRegistry
            .get("envforge.provisioning.duration")
            .tags(
                "outcome",
                "success",
                "template",
                "static_web"
            )
            .timer();

        assertThat(successTimer.count())
            .isEqualTo(1);
    }

    @Test
    void shouldMarkEnvironmentFailedWhenProvisioningFails() {
        EnvironmentEntity environment =
            createEnvironment("failed-demo");

        configureRepository(environment);

        doThrow(
            new IllegalStateException(
                "Helm installation failed"
            )
        )
            .when(environmentProvisioner)
            .provision(environment);

        environmentProvisioningService.provision(
            environment.getId()
        );

        assertThat(environment.getStatus())
            .isEqualTo(EnvironmentStatus.FAILED);

        verify(environmentProvisioner)
            .provision(environment);

        verify(environmentRepository, times(2))
            .saveAndFlush(environment);

        Counter failureCounter = meterRegistry
            .get("envforge.provisioning.attempts")
            .tags(
                "outcome",
                "failure",
                "template",
                "static_web"
            )
            .counter();

        assertThat(failureCounter.count())
            .isEqualTo(1.0);

        Timer failureTimer = meterRegistry
            .get("envforge.provisioning.duration")
            .tags(
                "outcome",
                "failure",
                "template",
                "static_web"
            )
            .timer();

        assertThat(failureTimer.count())
            .isEqualTo(1);
    }

    @Test
    void shouldRejectUnknownEnvironment() {
        UUID environmentId = UUID.randomUUID();

        when(
            environmentRepository.findById(environmentId)
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> environmentProvisioningService
                .provision(environmentId)
        )
            .isInstanceOf(
                EnvironmentNotFoundException.class
            )
            .hasMessage(
                "Environment not found: " + environmentId
            );

        verify(
            environmentRepository,
            never()
        ).saveAndFlush(any(EnvironmentEntity.class));

        verifyNoInteractions(environmentProvisioner);

        assertThat(
            meterRegistry
                .find("envforge.provisioning.attempts")
                .counter()
        ).isNull();

        assertThat(
            meterRegistry
                .find("envforge.provisioning.duration")
                .timer()
        ).isNull();
    }

    private void configureRepository(
        EnvironmentEntity environment
    ) {
        when(
            environmentRepository.findById(
                environment.getId()
            )
        ).thenReturn(Optional.of(environment));

        when(
            environmentRepository.saveAndFlush(
                any(EnvironmentEntity.class)
            )
        ).thenAnswer(
            invocation -> invocation.getArgument(0)
        );
    }

    private EnvironmentEntity createEnvironment(
        String name
    ) {
        Instant now =
            Instant.parse("2026-08-21T10:00:00Z");

        return new EnvironmentEntity(
            UUID.randomUUID(),
            name,
            "env-" + name,
            EnvironmentTemplate.STATIC_WEB,
            "0.2.0",
            1,
            ResourceProfile.SMALL,
            EnvironmentStatus.REQUESTED,
            true,
            "test-user",
            now,
            now.plus(2, ChronoUnit.HOURS),
            now
        );
    }
}