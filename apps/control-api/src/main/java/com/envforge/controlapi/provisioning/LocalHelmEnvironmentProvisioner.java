package com.envforge.controlapi.provisioning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.envforge.controlapi.environment.EnvironmentEntity;
import com.envforge.controlapi.environment.EnvironmentTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalHelmEnvironmentProvisioner
    implements EnvironmentProvisioner {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            LocalHelmEnvironmentProvisioner.class
        );

    private final String kubeContext;
    private final Path chartPath;

    public LocalHelmEnvironmentProvisioner(
        @Value("${envforge.provisioning.kube-context}")
        String kubeContext,
        @Value("${envforge.provisioning.chart-path}")
        Path chartPath
    ) {
        this.kubeContext = kubeContext;
        this.chartPath = chartPath
            .toAbsolutePath()
            .normalize();

        if (!Files.isDirectory(this.chartPath)) {
            LOGGER.warn(
                "Configured Helm chart path {} does not exist. Local "
                    + "environment provisioning will fail until "
                    + "envforge.provisioning.chart-path (or the "
                    + "ENVFORGE_HELM_CHART_PATH env var) points at a "
                    + "real envforge-workload chart directory. This is "
                    + "expected when the app is started from a working "
                    + "directory other than apps/control-api, e.g. a "
                    + "packaged jar or a container.",
                this.chartPath
            );
        }
    }

    @Override
    public void provision(EnvironmentEntity environment) {
        verifySupportedTemplate(environment.getTemplate());
        verifyChartPathExists();

        createNamespaceIfMissing(environment.getNamespace());
        configureNamespace(environment);
        installRelease(environment);
    }

    private void createNamespaceIfMissing(String namespace) {
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

        if (namespaceResult.exitCode() == 0) {
            return;
        }

        execute(
            List.of(
                "kubectl",
                "--context",
                kubeContext,
                "create",
                "namespace",
                namespace
            ),
            false
        );
    }

    private void configureNamespace(
        EnvironmentEntity environment
    ) {
        execute(
            List.of(
                "kubectl",
                "--context",
                kubeContext,
                "label",
                "namespace",
                environment.getNamespace(),
                "app.kubernetes.io/part-of=envforge",
                "envforge.io/managed=true",
                "envforge.io/owner="
                    + environment.getCreatedBy(),
                "--overwrite"
            ),
            false
        );

        execute(
            List.of(
                "kubectl",
                "--context",
                kubeContext,
                "annotate",
                "namespace",
                environment.getNamespace(),
                "envforge.io/expires-at="
                    + environment.getExpiresAt(),
                "--overwrite"
            ),
            false
        );
    }

    private void installRelease(
        EnvironmentEntity environment
    ) {
        String releaseName = environment.getName();

        List<String> command = new ArrayList<>(
            List.of(
                "helm",
                "upgrade",
                "--install",
                releaseName,
                chartPath.toString(),
                "--kube-context",
                kubeContext,
                "--namespace",
                environment.getNamespace(),
                "--set-string",
                "environment.name="
                    + environment.getName(),
                "--set-string",
                "environment.owner="
                    + environment.getCreatedBy(),
                "--set-string",
                "environment.template=static-web",
                "--set-string",
                "environment.expiresAt="
                    + environment.getExpiresAt(),
                "--set-string",
                "workload.image.repository="
                    + imageRepository(
                        environment.getTemplate()
                    ),
                "--set-string",
                "workload.image.tag="
                    + environment.getImageVersion(),
                "--set",
                "workload.replicas="
                    + environment.getReplicas(),
                "--wait",
                "--timeout",
                "2m"
            )
        );

        execute(command, false);
    }

    private String imageRepository(
        EnvironmentTemplate template
    ) {
        return switch (template) {
            case STATIC_WEB ->
                "envforge/static-web-demo";
            case RELIABILITY_API ->
                "envforge/reliability-demo-api";
        };
    }

    private void verifySupportedTemplate(
        EnvironmentTemplate template
    ) {
        if (template != EnvironmentTemplate.STATIC_WEB) {
            throw new IllegalArgumentException(
                "Local provisioning currently supports "
                    + "only STATIC_WEB environments"
            );
        }
    }

    private void verifyChartPathExists() {
        if (!Files.isDirectory(chartPath)) {
            throw new IllegalStateException(
                "Cannot provision environment: Helm chart path "
                    + chartPath
                    + " does not exist. Set "
                    + "envforge.provisioning.chart-path (or the "
                    + "ENVFORGE_HELM_CHART_PATH env var) to the "
                    + "envforge-workload chart directory."
            );
        }
    }

    private CommandResult execute(
        List<String> command,
        boolean allowFailure
    ) {
        LOGGER.info(
            "Executing provisioning command: {}",
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

            if (exitCode != 0 && !allowFailure) {
                throw new IllegalStateException(
                    "Provisioning command failed with exit "
                        + "code "
                        + exitCode
                        + ": "
                        + output
                );
            }

            return new CommandResult(exitCode, output);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Could not start provisioning command",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                "Provisioning command was interrupted",
                exception
            );
        }
    }

    private record CommandResult(
        int exitCode,
        String output
    ) {
    }
}