# Week 5 - Local Kind lifecycle execution

## Context

EnvForge no longer extends the Azure/AKS implementation for the next stages.
Existing Azure and Terraform work remains in the repository.

The local platform already uses:

- PostgreSQL through `docker compose`;
- Control API running from WSL;
- Kubernetes workloads on the `kind-envforge` cluster;
- the `deployment/helm/envforge-workload` chart.

For that reason cleanup-worker follows the same local architecture: it runs
from WSL against the shared PostgreSQL database and executes Helm/Kubernetes
operations against `kind-envforge`.

## Deliverables

- real Helm runner enabled only by configuration;
- explicit `kind-envforge` kube context;
- safe `ProcessBuilder` argument lists;
- Helm release and namespace validation;
- managed-namespace guard using `envforge.io/managed=true`;
- idempotent Helm uninstall;
- namespace deletion and deletion verification;
- rollback with a target Helm revision;
- command timeout;
- Docker image containing Java 21, Helm and kubectl;
- unit tests for command construction and validation.

## Real mode

The default remains safe dry-run mode.

Real Kind mode is enabled with:

```bash
ENVFORGE_LIFECYCLE_RUNNER_MODE=real
ENVFORGE_KUBE_CONTEXT=kind-envforge
```

## Definition of Done

- `mvn clean test` passes;
- `mvn clean package` passes;
- Docker image builds;
- Helm and kubectl exist in the image;
- unmanaged namespaces are rejected;
- repeated cleanup is idempotent;
- delete removes both the Helm release and the managed namespace;
- rollback uses the configured Kind context.
