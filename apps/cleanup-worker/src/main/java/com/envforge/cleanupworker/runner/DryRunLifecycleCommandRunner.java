package com.envforge.cleanupworker.runner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "envforge.lifecycle.runner",
        name = "mode",
        havingValue = "dry-run",
        matchIfMissing = true
)
public class DryRunLifecycleCommandRunner
        implements LifecycleCommandRunner {

    @Override
    public CommandResult verifyManagedNamespace(String namespaceName) {
        return CommandResult.success(
                "DRY RUN: managed namespace verified: " + namespaceName
        );
    }

    @Override
    public CommandResult uninstall(
            String releaseName,
            String namespaceName
    ) {
        return CommandResult.success(
                "DRY RUN: helm uninstall "
                        + releaseName
                        + " --namespace "
                        + namespaceName
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
    public CommandResult verifyCleanup(
            String releaseName,
            String namespaceName
    ) {
        return CommandResult.success(
                "DRY RUN: release cleanup verified for "
                        + releaseName
                        + " in "
                        + namespaceName
        );
    }

    @Override
    public CommandResult verifyRollback(
            String releaseName,
            String namespaceName
    ) {
        return CommandResult.success(
                "DRY RUN: rollback verified for "
                        + releaseName
                        + " in "
                        + namespaceName
        );
    }

    @Override
    public CommandResult deleteNamespace(String namespaceName) {
        return CommandResult.success(
                "DRY RUN: kubectl delete namespace " + namespaceName
        );
    }

    @Override
    public CommandResult verifyNamespaceDeleted(String namespaceName) {
        return CommandResult.success(
                "DRY RUN: namespace deletion verified: " + namespaceName
        );
    }
}
