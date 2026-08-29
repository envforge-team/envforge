package com.envforge.controlapi.lifecycle;

import com.envforge.controlapi.audit.AuditResult;
import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment.EnvironmentRepository;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LifecycleService {

    private final EnvironmentRepository environmentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;
    private final LifecycleWorkerClient workerClient;
    private final AuditService auditService;

    public LifecycleService(
        EnvironmentRepository environmentRepository,
        CurrentUserProvider currentUserProvider,
        AuthorizationService authorizationService,
        LifecycleWorkerClient workerClient,
        AuditService auditService
    ) {
        this.environmentRepository = environmentRepository;
        this.currentUserProvider = currentUserProvider;
        this.authorizationService = authorizationService;
        this.workerClient = workerClient;
        this.auditService = auditService;
    }

    public LifecycleJobResponse delete(UUID environmentId) {
        EnvironmentEntity environment =
            requireEnvironment(environmentId);

        CurrentUser user =
            currentUserProvider.getCurrentUser();

        authorizationService.requireOwnerOrAdmin(
            user,
            "DELETE_ENVIRONMENT",
            environment.getCreatedBy()
        );

        LifecycleJobResponse response =
            workerClient.createJob(
                environment.getId(),
                LifecycleAction.DELETE,
                null,
                user.email(),
                environment.getNamespace(),
                environment.getName()
            );

        auditService.record(
            user,
            "DELETE_ENVIRONMENT",
            "ENVIRONMENT",
            environment.getId().toString(),
            AuditResult.SUCCESS,
            "Lifecycle job queued: " + response.id()
        );

        return response;
    }

    public LifecycleJobResponse rollback(
        UUID environmentId,
        int targetRevision
    ) {
        EnvironmentEntity environment =
            requireEnvironment(environmentId);

        CurrentUser user =
            currentUserProvider.getCurrentUser();

        authorizationService.requireOwnerOrAdmin(
            user,
            "ROLLBACK_ENVIRONMENT",
            environment.getCreatedBy()
        );

        LifecycleJobResponse response =
            workerClient.createJob(
                environment.getId(),
                LifecycleAction.ROLLBACK,
                targetRevision,
                user.email(),
                environment.getNamespace(),
                environment.getName()
            );

        auditService.record(
            user,
            "ROLLBACK_ENVIRONMENT",
            "ENVIRONMENT",
            environment.getId().toString(),
            AuditResult.SUCCESS,
            "Rollback lifecycle job queued: " + response.id()
                + ", revision=" + targetRevision
        );

        return response;
    }

    private EnvironmentEntity requireEnvironment(UUID environmentId) {
        return environmentRepository.findById(environmentId)
            .orElseThrow(
                () -> new EnvironmentNotFoundException(
                    environmentId
                )
            );
    }
}
