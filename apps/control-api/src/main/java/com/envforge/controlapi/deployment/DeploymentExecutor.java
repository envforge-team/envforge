package com.envforge.controlapi.deployment;

import com.envforge.controlapi.environment.EnvironmentEntity;

public interface DeploymentExecutor {

    void deploy(
        EnvironmentEntity environment,
        String version
    );
}
