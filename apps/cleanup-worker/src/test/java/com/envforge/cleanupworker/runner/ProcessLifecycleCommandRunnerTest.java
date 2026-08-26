package com.envforge.cleanupworker.runner;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessLifecycleCommandRunnerTest {

    @Test
    void shouldBuildHelmUninstallCommandForKindContext() {
        CommandExecutor executor = mock(CommandExecutor.class);

        when(executor.execute(any(), any()))
                .thenReturn(CommandResult.success("namespace/demo-ns"))
                .thenReturn(CommandResult.success("[{\"name\":\"demo-release\"}]"))
                .thenReturn(CommandResult.success("uninstalled"));

        ProcessLifecycleCommandRunner runner =
                new ProcessLifecycleCommandRunner(
                        executor,
                        300,
                        "kind-envforge"
                );

        CommandResult result =
                runner.uninstall("demo-release", "demo-ns");

        assertTrue(result.successful());

        verify(executor).execute(
                List.of(
                        "helm",
                        "uninstall",
                        "demo-release",
                        "--kube-context",
                        "kind-envforge",
                        "--namespace",
                        "demo-ns",
                        "--wait",
                        "--timeout",
                        "300s"
                ),
                Duration.ofSeconds(310)
        );
    }

    @Test
    void shouldBuildNamespaceManagedCheck() {
        CommandExecutor executor = mock(CommandExecutor.class);

        when(executor.execute(any(), any()))
                .thenReturn(CommandResult.success("namespace/env-demo"))
                .thenReturn(CommandResult.success("true"));

        ProcessLifecycleCommandRunner runner =
                new ProcessLifecycleCommandRunner(
                        executor,
                        300,
                        "kind-envforge"
                );

        CommandResult result =
                runner.verifyManagedNamespace("env-demo");

        assertTrue(result.successful());

        verify(executor).execute(
                List.of(
                        "kubectl",
                        "--context",
                        "kind-envforge",
                        "get",
                        "namespace",
                        "env-demo",
                        "--output",
                        "jsonpath={.metadata.labels.envforge\\.io/managed}"
                ),
                Duration.ofSeconds(30)
        );
    }

    @Test
    void shouldRejectUnmanagedNamespace() {
        CommandExecutor executor = mock(CommandExecutor.class);

        when(executor.execute(any(), any()))
                .thenReturn(CommandResult.success("namespace/env-demo"))
                .thenReturn(CommandResult.success("false"));

        ProcessLifecycleCommandRunner runner =
                new ProcessLifecycleCommandRunner(
                        executor,
                        300,
                        "kind-envforge"
                );

        CommandResult result =
                runner.verifyManagedNamespace("env-demo");

        assertTrue(!result.successful());
    }

    @Test
    void shouldRejectInvalidNamespace() {
        CommandExecutor executor = mock(CommandExecutor.class);

        ProcessLifecycleCommandRunner runner =
                new ProcessLifecycleCommandRunner(
                        executor,
                        300,
                        "kind-envforge"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> runner.uninstall(
                        "demo-release",
                        "INVALID NAMESPACE"
                )
        );
    }

    @Test
    void shouldRejectInvalidRevision() {
        CommandExecutor executor = mock(CommandExecutor.class);

        ProcessLifecycleCommandRunner runner =
                new ProcessLifecycleCommandRunner(
                        executor,
                        300,
                        "kind-envforge"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> runner.rollback(
                        "demo-release",
                        "demo-ns",
                        0
                )
        );
    }
}
