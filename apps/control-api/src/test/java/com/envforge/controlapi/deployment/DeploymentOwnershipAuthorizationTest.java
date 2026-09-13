package com.envforge.controlapi.deployment;

import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;
import com.envforge.controlapi.security.AccessDeniedException;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import com.envforge.controlapi.security.SecurityMetrics;
import com.envforge.controlapi.user.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentOwnershipAuthorizationTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private DeploymentExecutor deploymentExecutor;

    @Mock
    private DeploymentMetrics deploymentMetrics;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityMetrics securityMetrics;

    private DeploymentService deploymentService;
    private EnvironmentEntity environment;
    private UUID environmentId;

    @BeforeEach
    void setUp() {
        AuthorizationService authorizationService =
            new AuthorizationService(
                auditService,
                securityMetrics
            );

        deploymentService = new DeploymentService(
            deploymentRepository,
            environmentRepository,
            deploymentExecutor,
            deploymentMetrics,
            currentUserProvider,
            authorizationService
        );

        environmentId = UUID.randomUUID();

        Instant now =
            Instant.parse(
                "2026-09-13T10:00:00Z"
            );

        environment = new EnvironmentEntity(
            environmentId,
            "owned-api",
            "env-owned-api",
            EnvironmentTemplate.STATIC_WEB,
            "0.1.0",
            1,
            ResourceProfile.SMALL,
            EnvironmentStatus.READY,
            true,
            "owner@example.test",
            now,
            now.plus(
                4,
                ChronoUnit.HOURS
            ),
            now
        );

        when(
            environmentRepository.findById(
                environmentId
            )
        ).thenReturn(
            Optional.of(environment)
        );
    }

    @Test
    void ownerOperatorShouldViewDeploymentHistory() {
        when(
            currentUserProvider.getCurrentUser()
        ).thenReturn(
            ownerOperator()
        );

        when(
            deploymentRepository
                .findByEnvironmentIdOrderByStartedAtDesc(
                    environmentId
                )
        ).thenReturn(
            List.of()
        );

        assertThat(
            deploymentService.getHistory(
                environmentId
            )
        ).isEmpty();
    }

    @Test
    void adminShouldViewDeploymentHistory() {
        when(
            currentUserProvider.getCurrentUser()
        ).thenReturn(
            new CurrentUser(
                "admin-id",
                "admin@example.test",
                "Admin",
                Role.ADMIN
            )
        );

        when(
            deploymentRepository
                .findByEnvironmentIdOrderByStartedAtDesc(
                    environmentId
                )
        ).thenReturn(
            List.of()
        );

        assertThat(
            deploymentService.getHistory(
                environmentId
            )
        ).isEmpty();
    }

    @Test
    void nonOwnerOperatorShouldNotViewDeploymentHistory() {
        when(
            currentUserProvider.getCurrentUser()
        ).thenReturn(
            new CurrentUser(
                "other-id",
                "other@example.test",
                "Other Operator",
                Role.OPERATOR
            )
        );

        assertThatThrownBy(
            () -> deploymentService.getHistory(
                environmentId
            )
        ).isInstanceOf(
            AccessDeniedException.class
        );
    }

    @Test
    void userShouldNotViewDeploymentHistoryEvenWhenOwner() {
        when(
            currentUserProvider.getCurrentUser()
        ).thenReturn(
            new CurrentUser(
                "owner-id",
                "owner@example.test",
                "Owner User",
                Role.USER
            )
        );

        assertThatThrownBy(
            () -> deploymentService.getHistory(
                environmentId
            )
        ).isInstanceOf(
            AccessDeniedException.class
        );
    }

    @Test
    void nonOwnerOperatorShouldNotUpdateEnvironment() {
        when(
            currentUserProvider.getCurrentUser()
        ).thenReturn(
            new CurrentUser(
                "other-id",
                "other@example.test",
                "Other Operator",
                Role.OPERATOR
            )
        );

        assertThatThrownBy(
            () -> deploymentService.triggerUpdate(
                environmentId,
                new UpdateEnvironmentRequest(
                    "0.2.0"
                )
            )
        ).isInstanceOf(
            AccessDeniedException.class
        );

        verifyNoInteractions(
            deploymentExecutor
        );
    }

    @Test
    void ownerOperatorShouldUpdateEnvironment() {
        when(
            currentUserProvider.getCurrentUser()
        ).thenReturn(
            ownerOperator()
        );

        when(
            environmentRepository.findByIdForUpdate(
                environmentId
            )
        ).thenReturn(
            Optional.of(environment)
        );

        when(
            deploymentRepository
                .findByEnvironmentIdOrderByStartedAtDesc(
                    environmentId
                )
        ).thenReturn(
            List.of()
        );

        DeploymentResponse response =
            deploymentService.triggerUpdate(
                environmentId,
                new UpdateEnvironmentRequest(
                    "0.2.0"
                )
            );

        assertThat(
            response.status()
        ).isEqualTo(
            DeploymentStatus.SUCCESS
        );

        assertThat(
            response.triggeredBy()
        ).isEqualTo(
            "owner@example.test"
        );
    }

    private CurrentUser ownerOperator() {
        return new CurrentUser(
            "owner-id",
            "owner@example.test",
            "Owner Operator",
            Role.OPERATOR
        );
    }
}
