package com.envforge.controlapi.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalCommandEnvironmentRuntimeInspector
    implements EnvironmentRuntimeInspector {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            LocalCommandEnvironmentRuntimeInspector.class
        );

    private final String kubeContext;

    public LocalCommandEnvironmentRuntimeInspector(
        @Value("${envforge.provisioning.kube-context}")
        String kubeContext
    ) {
        this.kubeContext = kubeContext;
    }

    @Override
    public EnvironmentRuntimeSnapshot inspect(
        String namespace,
        String releaseName
    ) {
        CommandResult namespaceResult = execute(
            List.of(
                "kubectl",
                "--context",
                kubeContext,
                "get",
                "namespace",
                namespace
            ),
            true
        );

        if (namespaceResult.exitCode() != 0) {
            return new EnvironmentRuntimeSnapshot(
                false,
                "not-found",
                null,
                null,
                null,
                null
            );
        }

        String helmStatus = inspectHelmStatus(
            namespace,
            releaseName
        );

        DeploymentDetails deployment =
            inspectDeployment(
                namespace,
                releaseName
            );

        String serviceName = inspectService(
            namespace,
            releaseName
        );

        return new EnvironmentRuntimeSnapshot(
            true,
            helmStatus,
            deployment.name(),
            deployment.desiredReplicas(),
            deployment.readyReplicas(),
            serviceName
        );
    }

    private String inspectHelmStatus(
        String namespace,
        String releaseName
    ) {
        CommandResult result = execute(
            List.of(
                "helm",
                "status",
                releaseName,
                "--kube-context",
                kubeContext,
                "--namespace",
                namespace
            ),
            true
        );

        if (result.exitCode() != 0) {
            return "not-found";
        }

        return result.output()
            .lines()
            .filter(
                line -> line.startsWith("STATUS:")
            )
            .map(
                line -> line
                    .substring("STATUS:".length())
                    .trim()
            )
            .findFirst()
            .orElse("unknown");
    }

    private DeploymentDetails inspectDeployment(
        String namespace,
        String releaseName
    ) {
        CommandResult result = execute(
            List.of(
                "kubectl",
                "--context",
                kubeContext,
                "--namespace",
                namespace,
                "get",
                "deployment",
                "--selector",
                "app.kubernetes.io/instance="
                    + releaseName,
                "--output",
                "jsonpath={.items[0].metadata.name}"
                    + "|{.items[0].spec.replicas}"
                    + "|{.items[0].status.readyReplicas}"
            ),
            true
        );

        if (
            result.exitCode() != 0 ||
            result.output().isBlank()
        ) {
            return DeploymentDetails.missing();
        }

        String[] values = result
            .output()
            .trim()
            .split("\\|", -1);

        if (values.length != 3) {
            return DeploymentDetails.missing();
        }

        return new DeploymentDetails(
            blankToNull(values[0]),
            parseInteger(values[1]),
            parseInteger(values[2])
        );
    }

    private String inspectService(
        String namespace,
        String releaseName
    ) {
        CommandResult result = execute(
            List.of(
                "kubectl",
                "--context",
                kubeContext,
                "--namespace",
                namespace,
                "get",
                "service",
                "--selector",
                "app.kubernetes.io/instance="
                    + releaseName,
                "--output",
                "jsonpath={.items[0].metadata.name}"
            ),
            true
        );

        if (result.exitCode() != 0) {
            return null;
        }

        return blankToNull(result.output());
    }

    private Integer parseInteger(String value) {
        String normalized = value.trim();

        if (normalized.isEmpty()) {
            return 0;
        }

        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException exception) {
            LOGGER.warn(
                "Could not parse Kubernetes replica value: {}",
                normalized
            );

            return null;
        }
    }

    private String blankToNull(String value) {
        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private CommandResult execute(
        List<String> command,
        boolean allowFailure
    ) {
        LOGGER.debug(
            "Executing runtime inspection command: {}",
            String.join(" ", command)
        );

        ProcessBuilder processBuilder =
            new ProcessBuilder(command);

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            String output = new String(
                process
                    .getInputStream()
                    .readAllBytes(),
                StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();

            if (exitCode != 0 && !allowFailure) {
                throw new IllegalStateException(
                    "Runtime inspection command failed "
                        + "with exit code "
                        + exitCode
                        + ": "
                        + output
                );
            }

            return new CommandResult(
                exitCode,
                output
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Could not start runtime inspection command",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                "Runtime inspection command was interrupted",
                exception
            );
        }
    }

    private record CommandResult(
        int exitCode,
        String output
    ) {
    }

    private record DeploymentDetails(
        String name,
        Integer desiredReplicas,
        Integer readyReplicas
    ) {
        private static DeploymentDetails missing() {
            return new DeploymentDetails(
                null,
                null,
                null
            );
        }
    }
}