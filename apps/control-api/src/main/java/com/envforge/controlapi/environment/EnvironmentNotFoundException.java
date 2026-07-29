package com.envforge.controlapi.environment;

import java.util.UUID;

public class EnvironmentNotFoundException
    extends RuntimeException {

    public EnvironmentNotFoundException(UUID id) {
        super("Environment not found: " + id);
    }
}