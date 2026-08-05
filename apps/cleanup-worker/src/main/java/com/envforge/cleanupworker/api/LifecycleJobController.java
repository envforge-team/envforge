package com.envforge.cleanupworker.api;

import com.envforge.cleanupworker.domain.LifecycleAction;
import com.envforge.cleanupworker.domain.LifecycleJobStatus;
import com.envforge.cleanupworker.persistence.entity.LifecycleJobEntity;
import com.envforge.cleanupworker.service.LifecycleJobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/lifecycle/jobs")
public class LifecycleJobController {

    private final LifecycleJobService jobService;

    public LifecycleJobController(LifecycleJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LifecycleJobResponse create(
            @Valid @RequestBody CreateLifecycleJobRequest request
    ) {
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
