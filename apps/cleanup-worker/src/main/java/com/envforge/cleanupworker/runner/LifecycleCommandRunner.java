package com.envforge.cleanupworker.runner;

public interface LifecycleCommandRunner {

    CommandResult verifyManagedNamespace(String namespaceName);

    CommandResult uninstall(String releaseName, String namespaceName);

    CommandResult rollback(
            String releaseName,
            String namespaceName,
            int revision
    );

    CommandResult verifyCleanup(
            String releaseName,
            String namespaceName
    );

    CommandResult verifyRollback(
            String releaseName,
            String namespaceName
    );

    CommandResult deleteNamespace(String namespaceName);

    CommandResult verifyNamespaceDeleted(String namespaceName);
}
