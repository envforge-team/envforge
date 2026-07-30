package com.envforge.controlapi.environment;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(
        EnvironmentService environmentService
    ) {
        this.environmentService = environmentService;
    }

    @PostMapping
    public ResponseEntity<EnvironmentResponse> create(
        @Valid @RequestBody CreateEnvironmentRequest request
    ) {
        EnvironmentResponse environment =
            environmentService.create(request);

        URI location = URI.create(
            "/api/environments/" + environment.id()
        );

        return ResponseEntity
            .created(location)
            .body(environment);
    }

    @GetMapping
    public ResponseEntity<List<EnvironmentResponse>> findAll() {
        return ResponseEntity.ok(
            environmentService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> findById(
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
            environmentService.findById(id)
        );
    }

}