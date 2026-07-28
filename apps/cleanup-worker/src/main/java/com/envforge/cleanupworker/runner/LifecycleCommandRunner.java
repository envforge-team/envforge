package com.envforge.cleanupworker.runner;

public interface LifecycleCommandRunner {

    CommandResult uninstall(String releaseName, String namespaceName);

    CommandResult rollback(String releaseName, String namespaceName, int revision);

    CommandResult verifyCleanup(String releaseName, String namespaceName);

    CommandResult verifyRollback(String releaseName, String namespaceName);
}
