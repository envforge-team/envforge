package com.envforge.controlapi.deployment;

public class DeploymentNotFoundException extends RuntimeException {
    public DeploymentNotFoundException(Long id) {
        super("Deployment not found: " + id);
    }
}

