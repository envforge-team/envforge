package com.envforge.controlapi.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.envforge.controlapi.environment.EnvironmentResponse;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EnvironmentRetryServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EnvironmentRetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new EnvironmentRetryService(
            environmentRepository,
            eventPublisher
        );
    }

    @Test
    void shouldRetryFailedEnvironment() {
        EnvironmentEntity environment =
            createEnvironment(
                EnvironmentStatus.FAILED
            );

        when(
            environmentRepository.findById(
                environment.getId()
            )
        ).thenReturn(Optional.of(environment));

        when(
            environmentRepository.save(
                any(EnvironmentEntity.class)
            )
        ).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        EnvironmentResponse response =
            retryService.retry(
                environment.getId()
            );

        assertThat(response.status())
            .isEqualTo(
                EnvironmentStatus.REQUESTED
            );

        assertThat(environment.getStatus())
            .isEqualTo(
                EnvironmentStatus.REQUESTED
            );

        verify(environmentRepository)
            .save(environment);

        verify(eventPublisher)
            .publishEvent(
                any(
                    EnvironmentRequestedEvent.class
                )
            );
    }

    @Test
    void shouldRejectRetryForReadyEnvironment() {
        EnvironmentEntity environment =
            createEnvironment(
                EnvironmentStatus.READY
            );

        when(
            environmentRepository.findById(
                environment.getId()
            )
        ).thenReturn(Optional.of(environment));

        assertThatThrownBy(
            () -> retryService.retry(
                environment.getId()
            )
        )
            .isInstanceOf(
                EnvironmentRetryNotAllowedException.class
            )
            .hasMessageContaining(
                "cannot be retried from status READY"
            );

        verify(
            environmentRepository,
            never()
        ).save(any(EnvironmentEntity.class));

        verifyNoInteractions(eventPublisher);
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
            () -> retryService.retry(
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

        verifyNoInteractions(eventPublisher);
    }

    private EnvironmentEntity createEnvironment(
        EnvironmentStatus status
    ) {
        Instant now =
            Instant.parse("2026-08-26T10:00:00Z");

        return new EnvironmentEntity(
            UUID.randomUUID(),
            "retry-demo",
            "env-retry-demo",
            EnvironmentTemplate.STATIC_WEB,
            "0.2.0",
            1,
            ResourceProfile.SMALL,
            status,
            true,
            "test-user",
            now,
            now.plus(2, ChronoUnit.HOURS),
            now
        );
    }
}