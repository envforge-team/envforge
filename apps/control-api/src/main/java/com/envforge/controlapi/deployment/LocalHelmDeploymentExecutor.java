package com.envforge.controlapi.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.envforge.controlapi.environment.EnvironmentEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalHelmDeploymentExecutor
    implements DeploymentExecutor {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            LocalHelmDeploymentExecutor.class
        );

    private final String kubeContext;
    private final Path chartPath;

    public LocalHelmDeploymentExecutor(
        @Value("${envforge.provisioning.kube-context}")
        String kubeContext,
        @Value("${envforge.provisioning.chart-path}")
        String chartPath
    ) {
        this.kubeContext = kubeContext;
        this.chartPath = Path.of(chartPath)
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public void deploy(
        EnvironmentEntity environment,
        String version
    ) {
        verifyChartPath();

        run(
            List.of(
                "helm",
                "upgrade",
                environment.getName(),
                chartPath.toString(),
                "--kube-context",
                kubeContext,
                "--namespace",
                environment.getNamespace(),
                "--reuse-values",
                "--set-string",
                "workload.image.tag=" + version,
                "--wait",
                "--timeout",
                "2m"
            )
        );

        String deploymentName =
            environment.getName()
                + "-envforge-workload";

        run(
            List.of(
                "kubectl",
                "--context",
                kubeContext,
                "rollout",
                "status",
                "deployment/" + deploymentName,
                "--namespace",
                environment.getNamespace(),
                "--timeout=120s"
            )
        );
    }

    private void verifyChartPath() {
        if (!Files.isDirectory(chartPath)) {
            throw new IllegalStateException(
                "Deployment Helm chart path does not exist: "
                    + chartPath
            );
        }
    }

    private void run(List<String> command) {
        LOGGER.info(
            "Executing deployment command: {}",
            String.join(" ", command)
        );

        ProcessBuilder processBuilder =
            new ProcessBuilder(command);

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException(
                    "Deployment command failed with exit "
                        + "code "
                        + exitCode
                        + ": "
                        + output
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Could not start deployment command",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                "Deployment command was interrupted",
                exception
            );
        }
    }
}
