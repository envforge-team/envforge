package com.envforge.controlapi.lifecycle;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/environments")
public class LifecycleController {

    private final LifecycleService lifecycleService;

    public LifecycleController(
        LifecycleService lifecycleService
    ) {
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<LifecycleJobResponse> delete(
        @PathVariable UUID id
    ) {
        return ResponseEntity.accepted().body(
            lifecycleService.delete(id)
        );
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<LifecycleJobResponse> rollback(
        @PathVariable UUID id,
        @Valid @RequestBody
        RollbackEnvironmentRequest request
    ) {
        return ResponseEntity.accepted().body(
            lifecycleService.rollback(
                id,
                request.targetRevision()
            )
        );
    }
}
