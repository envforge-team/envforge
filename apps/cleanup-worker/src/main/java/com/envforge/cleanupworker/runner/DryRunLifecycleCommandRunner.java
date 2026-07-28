package com.envforge.cleanupworker.runner;

import org.springframework.stereotype.Component;

@Component
public class DryRunLifecycleCommandRunner implements LifecycleCommandRunner {

    @Override
    public CommandResult uninstall(String releaseName, String namespaceName) {
        return CommandResult.success(
                "DRY RUN: helm uninstall " + releaseName + " --namespace " + namespaceName
        );
    }

    @Override
    public CommandResult rollback(
            String releaseName,
            String namespaceName,
            int revision
    ) {
        return CommandResult.success(
                "DRY RUN: helm rollback "
                        + releaseName
                        + " "
                        + revision
                        + " --namespace "
                        + namespaceName
        );
    }

    @Override
    public CommandResult verifyCleanup(String releaseName, String namespaceName) {
        return CommandResult.success(
                "DRY RUN: cleanup verification for "
                        + releaseName
                        + " in "
                        + namespaceName
        );
    }

    @Override
    public CommandResult verifyRollback(String releaseName, String namespaceName) {
        return CommandResult.success(
                "DRY RUN: rollback verification for "
                        + releaseName
                        + " in "
                        + namespaceName
        );
    }
}
