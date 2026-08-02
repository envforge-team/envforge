package com.envforge.controlapi.deployment;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/environments/{environmentId}")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PatchMapping
    public ResponseEntity<DeploymentResponse> updateEnvironment(
            @PathVariable UUID environmentId,
            @Valid @RequestBody UpdateEnvironmentRequest request
    ) {
        return ResponseEntity.ok(deploymentService.triggerUpdate(environmentId, request));
    }

    @GetMapping("/deployments")
    public ResponseEntity<List<DeploymentResponse>> getDeploymentHistory(
            @PathVariable UUID environmentId
    ) {
        return ResponseEntity.ok(deploymentService.getHistory(environmentId));
    }
}