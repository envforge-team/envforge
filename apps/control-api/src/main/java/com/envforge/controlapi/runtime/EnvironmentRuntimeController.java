package com.envforge.controlapi.runtime;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/environments/{environmentId}/runtime"
)
public class EnvironmentRuntimeController {

    private final EnvironmentRuntimeService
        runtimeService;

    public EnvironmentRuntimeController(
        EnvironmentRuntimeService runtimeService
    ) {
        this.runtimeService = runtimeService;
    }

    @GetMapping
    public ResponseEntity<EnvironmentRuntimeResponse>
        inspect(
            @PathVariable UUID environmentId
        ) {
        return ResponseEntity.ok(
            runtimeService.inspect(environmentId)
        );
    }
}