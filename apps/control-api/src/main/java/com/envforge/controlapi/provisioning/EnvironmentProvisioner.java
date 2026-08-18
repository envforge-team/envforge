package com.envforge.controlapi.provisioning;

import com.envforge.controlapi.environment.EnvironmentEntity;

public interface EnvironmentProvisioner {

    void provision(EnvironmentEntity environment);
}