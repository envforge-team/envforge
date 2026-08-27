package com.envforge.controlapi.provisioning;

import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/environments/{environmentId}/retry"
)
public class EnvironmentProvisioningController {

    private final EnvironmentRetryService
        environmentRetryService;

    public EnvironmentProvisioningController(
        EnvironmentRetryService environmentRetryService
    ) {
        this.environmentRetryService =
            environmentRetryService;
    }

    @PostMapping
    public ResponseEntity<EnvironmentResponse> retry(
        @PathVariable UUID environmentId
    ) {
        return ResponseEntity
            .accepted()
            .body(
                environmentRetryService.retry(
                    environmentId
                )
            );
    }
}