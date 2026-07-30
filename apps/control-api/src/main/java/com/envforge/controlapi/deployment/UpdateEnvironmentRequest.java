package com.envforge.controlapi.deployment;

import jakarta.validation.constraints.NotBlank;

public record UpdateEnvironmentRequest(@NotBlank String version) {}