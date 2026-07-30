package com.envforge.controlapi.deployment;

public class InvalidVersionException extends RuntimeException {
    public InvalidVersionException(String version) {
        super("Invalid version: " + version);
    }
}

