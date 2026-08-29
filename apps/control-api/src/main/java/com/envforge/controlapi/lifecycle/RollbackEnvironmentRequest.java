package com.envforge.controlapi.lifecycle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RollbackEnvironmentRequest(
    @NotNull
    @Min(1)
    Integer targetRevision
) {
}
