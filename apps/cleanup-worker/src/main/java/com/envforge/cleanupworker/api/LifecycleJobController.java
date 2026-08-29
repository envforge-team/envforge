package com.envforge.cleanupworker.api;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.service.LifecycleJobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/lifecycle/jobs")
public class LifecycleJobController {

    private final LifecycleJobService jobService;
    private final String internalToken;

    public LifecycleJobController(
        LifecycleJobService jobService,
        @Value(
            "${envforge.lifecycle.internal-token:local-dev-internal-token}"
        )
        String internalToken
    ) {
        this.jobService = jobService;
        this.internalToken = internalToken;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LifecycleJobResponse create(
        @RequestHeader(
            value = "X-EnvForge-Internal-Token",
            required = false
        )
        String providedToken,
        @Valid @RequestBody
        CreateLifecycleJobRequest request
    ) {
        requireInternalToken(providedToken);

        LifecycleJobEntity job = jobService.createJob(
            request.environmentId(),
            request.action(),
            request.targetRevision(),
            request.actorId(),
            request.namespaceName(),
            request.helmReleaseName()
        );

        return new LifecycleJobResponse(
            job.getId(),
            job.getEnvironmentId(),
            job.getAction(),
            job.getStatus(),
            job.getAttemptCount()
        );
    }

    private void requireInternalToken(String providedToken) {
        if (!Objects.equals(internalToken, providedToken)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Invalid internal lifecycle token"
            );
        }
    }

    public record CreateLifecycleJobRequest(
        @NotNull UUID environmentId,
        @NotNull LifecycleAction action,
        Integer targetRevision,
        @NotBlank String actorId,
        @NotBlank String namespaceName,
        @NotBlank String helmReleaseName
    ) {
    }

    public record LifecycleJobResponse(
        UUID id,
        UUID environmentId,
        LifecycleAction action,
        LifecycleJobStatus status,
        int attemptCount
    ) {
    }
}
