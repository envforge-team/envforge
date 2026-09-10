# Day 26 - Deployment CI access status

## Original objective

The original M2 objective for Day 26 was to verify GitHub OIDC access
to Azure Container Registry (ACR) and Azure Kubernetes Service (AKS).

## Current project runtime

EnvForge currently uses a local Kind Kubernetes environment as the
functional deployment target.

Azure AKS/ACR infrastructure remains represented as infrastructure as
code, but the team does not rely on a live Azure deployment for the
functional project workflow.

Because of this, GitHub OIDC authentication to Azure is not required
for the active M2 deployment path.

## Active deployment path

The current deployment flow is:

Docker build
-> immutable local image tag
-> load image into Kind
-> Helm upgrade
-> kubectl rollout status
-> deployment history

The Control API performs environment updates using the configured Kind
context and the envforge-workload Helm chart.

## CI access model

GitHub-hosted runners cannot access the developer's local Kind cluster.

For CI validation, workflows must create their own temporary Kind
cluster when Kubernetes execution is required.

Local demo and end-to-end deployment validation use:

- Kubernetes context: kind-envforge-local
- Helm
- kubectl
- locally built Docker images loaded with kind load docker-image

No Azure credentials, client secrets, or simulated OIDC credentials
are required for the Kind path.

## Security decision

EnvForge does not introduce fake Azure credentials or local OIDC
emulation.

If Azure deployment becomes available later, the existing Azure OIDC
infrastructure can be validated separately against real Azure
resources.

## Day 26 result

Status: SUPERSEDED BY KIND

The active deployment workflow has a valid authentication/access model:

- local Kubernetes access through kubeconfig
- CI Kubernetes access through an ephemeral Kind cluster
- no dependency on Azure secrets
- no dependency on ACR or AKS for functional validation
