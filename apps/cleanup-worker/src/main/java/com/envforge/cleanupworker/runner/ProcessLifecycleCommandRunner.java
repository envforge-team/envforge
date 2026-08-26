package com.envforge.cleanupworker.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(
        prefix = "envforge.lifecycle.runner",
        name = "mode",
        havingValue = "real"
)
public class ProcessLifecycleCommandRunner
        implements LifecycleCommandRunner {

    private static final Pattern DNS_LABEL =
            Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");

    private final CommandExecutor commandExecutor;
    private final Duration timeout;
    private final String kubeContext;

    public ProcessLifecycleCommandRunner(
            CommandExecutor commandExecutor,
            @Value("${envforge.lifecycle.command-timeout-seconds:300}")
            long timeoutSeconds,
            @Value("${envforge.lifecycle.kube-context:kind-envforge}")
            String kubeContext
    ) {
        this.commandExecutor = commandExecutor;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.kubeContext = kubeContext;
    }

    @Override
    public CommandResult verifyManagedNamespace(String namespaceName) {
        validateNamespaceName(namespaceName);

        CommandResult lookup = lookupNamespace(namespaceName);

        if (!lookup.successful()) {
            return lookup;
        }

        if (lookup.output().isBlank()) {
            return CommandResult.success(
                    "Namespace already absent: " + namespaceName
            );
        }

        CommandResult labelResult = commandExecutor.execute(
                List.of(
                        "kubectl",
                        "--context",
                        kubeContext,
                        "get",
                        "namespace",
                        namespaceName,
                        "--output",
                        "jsonpath={.metadata.labels.envforge\\.io/managed}"
                ),
                Duration.ofSeconds(30)
        );

        if (!labelResult.successful()) {
            return labelResult;
        }

        if (!"true".equals(labelResult.output().trim())) {
            return CommandResult.failure(
                    6,
                    "Refusing lifecycle cleanup for unmanaged namespace: "
                            + namespaceName
            );
        }

        return CommandResult.success(
                "Managed namespace verified: " + namespaceName
        );
    }

    @Override
    public CommandResult uninstall(
            String releaseName,
            String namespaceName
    ) {
        validateReleaseName(releaseName);
        validateNamespaceName(namespaceName);

        CommandResult namespaceLookup =
                lookupNamespace(namespaceName);

        if (!namespaceLookup.successful()) {
            return namespaceLookup;
        }

        if (namespaceLookup.output().isBlank()) {
            return CommandResult.success(
                    "Namespace already absent; release is absent: "
                            + releaseName
            );
        }

        CommandResult existing = listRelease(
                releaseName,
                namespaceName
        );

        if (!existing.successful()) {
            return existing;
        }

        if ("[]".equals(existing.output().trim())) {
            return CommandResult.success(
                    "Helm release already absent: " + releaseName
            );
        }

        return commandExecutor.execute(
                List.of(
                        "helm",
                        "uninstall",
                        releaseName,
                        "--kube-context",
                        kubeContext,
                        "--namespace",
                        namespaceName,
                        "--wait",
                        "--timeout",
                        timeout.toSeconds() + "s"
                ),
                timeout.plusSeconds(10)
        );
    }

    @Override
    public CommandResult rollback(
            String releaseName,
            String namespaceName,
            int revision
    ) {
        validateReleaseName(releaseName);
        validateNamespaceName(namespaceName);

        if (revision < 1) {
            throw new IllegalArgumentException(
                    "Helm revision must be positive"
            );
        }

        return commandExecutor.execute(
                List.of(
                        "helm",
                        "rollback",
                        releaseName,
                        Integer.toString(revision),
                        "--kube-context",
                        kubeContext,
                        "--namespace",
                        namespaceName,
                        "--wait",
                        "--timeout",
                        timeout.toSeconds() + "s"
                ),
                timeout.plusSeconds(10)
        );
    }

    @Override
    public CommandResult verifyCleanup(
            String releaseName,
            String namespaceName
    ) {
        validateReleaseName(releaseName);
        validateNamespaceName(namespaceName);

        CommandResult namespaceLookup =
                lookupNamespace(namespaceName);

        if (!namespaceLookup.successful()) {
            return namespaceLookup;
        }

        if (namespaceLookup.output().isBlank()) {
            return CommandResult.success(
                    "Namespace already absent; release is absent: "
                            + releaseName
            );
        }

        CommandResult result = listRelease(
                releaseName,
                namespaceName
        );

        if (!result.successful()) {
            return result;
        }

        if ("[]".equals(result.output().trim())) {
            return CommandResult.success(
                    "Helm release no longer exists"
            );
        }

        return CommandResult.failure(
                7,
                "Helm release still exists after uninstall: "
                        + releaseName
        );
    }

    @Override
    public CommandResult verifyRollback(
            String releaseName,
            String namespaceName
    ) {
        validateReleaseName(releaseName);
        validateNamespaceName(namespaceName);

        return commandExecutor.execute(
                List.of(
                        "helm",
                        "status",
                        releaseName,
                        "--kube-context",
                        kubeContext,
                        "--namespace",
                        namespaceName,
                        "--output",
                        "json"
                ),
                Duration.ofSeconds(30)
        );
    }

    @Override
    public CommandResult deleteNamespace(String namespaceName) {
        validateNamespaceName(namespaceName);

        CommandResult lookup = lookupNamespace(namespaceName);

        if (!lookup.successful()) {
            return lookup;
        }

        if (lookup.output().isBlank()) {
            return CommandResult.success(
                    "Namespace already absent: " + namespaceName
            );
        }

        return commandExecutor.execute(
                List.of(
                        "kubectl",
                        "--context",
                        kubeContext,
                        "delete",
                        "namespace",
                        namespaceName,
                        "--wait=false"
                ),
                Duration.ofSeconds(30)
        );
    }

    @Override
    public CommandResult verifyNamespaceDeleted(String namespaceName) {
        validateNamespaceName(namespaceName);

        CommandResult lookup = lookupNamespace(namespaceName);

        if (!lookup.successful()) {
            return lookup;
        }

        if (lookup.output().isBlank()) {
            return CommandResult.success(
                    "Namespace deleted: " + namespaceName
            );
        }

        CommandResult waitResult = commandExecutor.execute(
                List.of(
                        "kubectl",
                        "--context",
                        kubeContext,
                        "wait",
                        "--for=delete",
                        "namespace/" + namespaceName,
                        "--timeout=" + timeout.toSeconds() + "s"
                ),
                timeout.plusSeconds(10)
        );

        if (waitResult.successful()) {
            return waitResult;
        }

        CommandResult finalLookup = lookupNamespace(namespaceName);

        if (finalLookup.successful()
                && finalLookup.output().isBlank()) {
            return CommandResult.success(
                    "Namespace deleted: " + namespaceName
            );
        }

        return waitResult;
    }

    private CommandResult listRelease(
            String releaseName,
            String namespaceName
    ) {
        return commandExecutor.execute(
                List.of(
                        "helm",
                        "list",
                        "--kube-context",
                        kubeContext,
                        "--namespace",
                        namespaceName,
                        "--filter",
                        "^" + releaseName + "$",
                        "--output",
                        "json"
                ),
                Duration.ofSeconds(30)
        );
    }

    private CommandResult lookupNamespace(String namespaceName) {
        return commandExecutor.execute(
                List.of(
                        "kubectl",
                        "--context",
                        kubeContext,
                        "get",
                        "namespace",
                        namespaceName,
                        "--ignore-not-found",
                        "--output",
                        "name"
                ),
                Duration.ofSeconds(30)
        );
    }

    private void validateNamespaceName(String value) {
        validateDnsLabel(value, 63, "namespace");
    }

    private void validateReleaseName(String value) {
        validateDnsLabel(value, 53, "Helm release");
    }

    private void validateDnsLabel(
            String value,
            int maxLength,
            String field
    ) {
        if (value == null
                || value.isBlank()
                || value.length() > maxLength
                || !DNS_LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid " + field + " name: " + value
            );
        }
    }
}
