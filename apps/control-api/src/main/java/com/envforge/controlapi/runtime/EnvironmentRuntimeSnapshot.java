package com.envforge.controlapi.runtime;

public record EnvironmentRuntimeSnapshot(
    boolean namespaceExists,
    String helmStatus,
    String deploymentName,
    Integer desiredReplicas,
    Integer readyReplicas,
    String serviceName
) {
}

