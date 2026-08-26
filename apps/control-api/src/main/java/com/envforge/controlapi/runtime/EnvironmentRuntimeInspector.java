
package com.envforge.controlapi.runtime;

public interface EnvironmentRuntimeInspector {

    EnvironmentRuntimeSnapshot inspect(
        String namespace,
        String releaseName
    );
}