package com.envforge.controlapi.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.environment.EnvironmentStatus;
import com.envforge.controlapi.environment.EnvironmentTemplate;
import com.envforge.controlapi.environment.ResourceProfile;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import com.envforge.controlapi.user.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LifecycleServiceTest {

    private EnvironmentRepository environmentRepository;
    private CurrentUserProvider currentUserProvider;
    private AuthorizationService authorizationService;
    private LifecycleWorkerClient workerClient;
    private AuditService auditService;
    private LifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        environmentRepository =
            Mockito.mock(EnvironmentRepository.class);
        currentUserProvider =
            Mockito.mock(CurrentUserProvider.class);
        authorizationService =
            Mockito.mock(AuthorizationService.class);
        workerClient =
            Mockito.mock(LifecycleWorkerClient.class);
        auditService =
            Mockito.mock(AuditService.class);

        lifecycleService = new LifecycleService(
            environmentRepository,
            currentUserProvider,
            authorizationService,
            workerClient,
            auditService
        );
    }

    @Test
    void shouldQueueDeleteWithCurrentActor() {
        UUID environmentId = UUID.randomUUID();
        EnvironmentEntity environment =
            environment(environmentId);

        CurrentUser owner = new CurrentUser(
            "owner-id",
            "owner@example.test",
            "Owner",
            Role.OPERATOR
        );

        LifecycleJobResponse expected =
            new LifecycleJobResponse(
                UUID.randomUUID(),
                environmentId,
                "DELETE",
                "QUEUED",
                0
            );

        when(environmentRepository.findById(environmentId))
            .thenReturn(Optional.of(environment));
        when(currentUserProvider.getCurrentUser())
            .thenReturn(owner);
        when(
            workerClient.createJob(
                environmentId,
                LifecycleAction.DELETE,
                null,
                owner.email(),
                environment.getNamespace(),
                environment.getName()
            )
        ).thenReturn(expected);

        LifecycleJobResponse response =
            lifecycleService.delete(environmentId);

        assertThat(response).isEqualTo(expected);

        verify(authorizationService)
            .requireOwnerOrAdmin(
                owner,
                "DELETE_ENVIRONMENT",
                "owner-id"
            );
    }

    @Test
    void shouldQueueRollbackWithRequestedRevision() {
        UUID environmentId = UUID.randomUUID();
        EnvironmentEntity environment =
            environment(environmentId);

        CurrentUser admin = new CurrentUser(
            "admin-id",
            "admin@example.test",
            "Admin",
            Role.ADMIN
        );

        LifecycleJobResponse expected =
            new LifecycleJobResponse(
                UUID.randomUUID(),
                environmentId,
                "ROLLBACK",
                "QUEUED",
                0
            );

        when(environmentRepository.findById(environmentId))
            .thenReturn(Optional.of(environment));
        when(currentUserProvider.getCurrentUser())
            .thenReturn(admin);
        when(
            workerClient.createJob(
                environmentId,
                LifecycleAction.ROLLBACK,
                1,
                admin.email(),
                environment.getNamespace(),
                environment.getName()
            )
        ).thenReturn(expected);

        LifecycleJobResponse response =
            lifecycleService.rollback(
                environmentId,
                1
            );

        assertThat(response).isEqualTo(expected);

        verify(authorizationService)
            .requireOwnerOrAdmin(
                admin,
                "ROLLBACK_ENVIRONMENT",
                "owner-id"
            );
    }

    private EnvironmentEntity environment(UUID id) {
        Instant now = Instant.now();

        return new EnvironmentEntity(
            id,
            "demo",
            "env-demo",
            EnvironmentTemplate.STATIC_WEB,
            "0.2.0",
            1,
            ResourceProfile.SMALL,
            EnvironmentStatus.READY,
            true,
            "owner-id",
            now,
            now.plusSeconds(3600),
            now
        );
    }
}
