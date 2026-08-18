package com.envforge.controlapi.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.envforge.controlapi.provisioning.EnvironmentRequestedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EnvironmentService environmentService;

    @BeforeEach
    void setUp() {
        environmentService = new EnvironmentService(
            environmentRepository,
            eventPublisher
        );
    }

    @Test
    void shouldCreateRequestedEnvironment() {
        CreateEnvironmentRequest request =
            new CreateEnvironmentRequest(
                "static-demo-test",
                EnvironmentTemplate.STATIC_WEB,
                "0.1.0",
                2,
                ResourceProfile.SMALL,
                4,
                true
            );

        when(
            environmentRepository.existsByName(
                request.name()
            )
        ).thenReturn(false);

        when(
            environmentRepository.existsByNamespace(
                "env-static-demo-test"
            )
        ).thenReturn(false);

        when(
            environmentRepository.save(
                any(EnvironmentEntity.class)
            )
        ).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        EnvironmentResponse response =
            environmentService.create(request);

        assertThat(response.id()).isNotNull();

        assertThat(response.name())
            .isEqualTo("static-demo-test");

        assertThat(response.namespace())
            .isEqualTo("env-static-demo-test");

        assertThat(response.template())
            .isEqualTo(EnvironmentTemplate.STATIC_WEB);

        assertThat(response.imageVersion())
            .isEqualTo("0.1.0");

        assertThat(response.replicas()).isEqualTo(2);

        assertThat(response.resourceProfile())
            .isEqualTo(ResourceProfile.SMALL);

        assertThat(response.status())
            .isEqualTo(EnvironmentStatus.REQUESTED);

        assertThat(response.monitoringEnabled()).isTrue();

        assertThat(response.createdBy())
            .isEqualTo("local-user");

        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        assertThat(response.expiresAt())
            .isAfter(response.createdAt());

        ArgumentCaptor<EnvironmentEntity> captor =
            ArgumentCaptor.forClass(
                EnvironmentEntity.class
            );

        verify(environmentRepository).save(
            captor.capture()
        );

        EnvironmentEntity saved = captor.getValue();

        assertThat(saved.getName())
            .isEqualTo("static-demo-test");

        assertThat(saved.getNamespace())
            .isEqualTo("env-static-demo-test");

        assertThat(saved.getStatus())
            .isEqualTo(EnvironmentStatus.REQUESTED);

        verify(eventPublisher).publishEvent(
            any(EnvironmentRequestedEvent.class)
        );
    }

    @Test
    void shouldRejectDuplicateEnvironmentName() {
        CreateEnvironmentRequest request =
            new CreateEnvironmentRequest(
                "duplicate-demo",
                EnvironmentTemplate.STATIC_WEB,
                "0.1.0",
                1,
                ResourceProfile.SMALL,
                2,
                true
            );

        when(
            environmentRepository.existsByName(
                "duplicate-demo"
            )
        ).thenReturn(true);

        assertThatThrownBy(
            () -> environmentService.create(request)
        )
            .isInstanceOf(
                EnvironmentAlreadyExistsException.class
            )
            .hasMessage(
                "Environment already exists: duplicate-demo"
            );

        verify(environmentRepository, never())
            .save(any(EnvironmentEntity.class));

        verify(eventPublisher, never())
            .publishEvent(any());
    }

    @Test
    void shouldFindEnvironmentById() {
        UUID id = UUID.randomUUID();

        EnvironmentEntity entity =
            TestEnvironmentFactory.environment(
                id,
                "existing-demo"
            );

        when(environmentRepository.findById(id))
            .thenReturn(java.util.Optional.of(entity));

        EnvironmentResponse response =
            environmentService.findById(id);

        assertThat(response.id()).isEqualTo(id);

        assertThat(response.name())
            .isEqualTo("existing-demo");
    }

    @Test
    void shouldThrowWhenEnvironmentDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(environmentRepository.findById(id))
            .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(
            () -> environmentService.findById(id)
        )
            .isInstanceOf(
                EnvironmentNotFoundException.class
            )
            .hasMessage(
                "Environment not found: " + id
            );
    }
}