package com.envforge.controlapi.environment;

public class EnvironmentAlreadyExistsException
    extends RuntimeException {

    public EnvironmentAlreadyExistsException(String name) {
        super("Environment already exists: " + name);
    }
}