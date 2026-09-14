# Deployment and Image Versioning

This document describes how EnvForge updates application versions,
records deployment history and handles rollout failures.

## Deployment flow

The environment update flow is:

```text
Select version
    |
    v
PATCH /api/environments/{id}
    |
    v
Authorization and concurrency checks
    |
    v
Deployment history -> IN_PROGRESS
Environment -> DEPLOYING
    |
    v
helm upgrade
    |
    v
kubectl rollout status
    |
    +-------------------------+
    |                         |
    v                         v
SUCCESS                     FAILURE
    |                         |
Environment -> READY       Environment -> DEGRADED
imageVersion updated       previous imageVersion preserved
```

The Control API performs the Helm and Kubernetes operations outside
the database transaction used to claim the rollout.

This avoids holding a database transaction while an external
deployment command is running.

## Update API

Update an existing environment:

```http
PATCH /api/environments/{id}
Content-Type: application/json

{
  "version": "0.2.1"
}
```

The requested version must use the expected `major.minor.patch`
format.

A successful request produces a deployment history entry containing:

- requested version
- image tag
- deployment status
- timestamps
- authenticated deployment actor

Deployment history can be queried through:

```http
GET /api/environments/{id}/deployments
```

## Image versioning

EnvForge uses fixed application image versions instead of mutable
`latest` tags.

For the static web workload, a version such as:

```text
0.2.1
```

maps to:

```text
envforge/static-web-demo:0.2.1
```

The workload Helm chart receives the version through:

```text
workload.image.tag
```

For example:

```bash
helm upgrade \
  my-environment \
  deployment/helm/envforge-workload \
  --namespace env-my-environment \
  --reuse-values \
  --set-string workload.image.tag=0.2.1 \
  --wait \
  --timeout 2m
```

Both the generic workload chart and the reliability demo chart reject
the `latest` tag during Helm rendering.

This keeps deployment history reproducible because a recorded version
continues to identify the intended application image.

## Environment states

Deployment updates use the following environment states.

### READY

The environment completed provisioning or a deployment update
successfully.

The stored `imageVersion` represents the last successfully deployed
version.

### DEPLOYING

A version update has been claimed and the rollout is currently in
progress.

### DEGRADED

A version update failed after an environment had previously been
usable.

The failed deployment is recorded as `FAILED`, while the environment
is marked `DEGRADED`.

The previous successful `imageVersion` is preserved in the database.

`DEGRADED` is intentionally different from provisioning `FAILED`.

A failed update does not mean that the environment was never
successfully provisioned.

### FAILED

`FAILED` is used for failures in the environment provisioning flow.

Provisioning retry operates on environments in this state.

## Deployment states

Deployment history supports the following status values:

```text
PENDING
IN_PROGRESS
SUCCESS
FAILED
ROLLED_BACK
```

The current environment update path normally transitions:

```text
IN_PROGRESS -> SUCCESS
```

or:

```text
IN_PROGRESS -> FAILED
```

A failed Helm upgrade is not reported as `ROLLED_BACK` unless an
actual rollback mechanism is performed.

The current local deployment executor does not claim that a failed
upgrade was automatically rolled back.

## Failure and recovery

A typical failure scenario is:

```text
Current version: 0.2.1
Requested version: 9.9.9
```

If `9.9.9` cannot be deployed:

```text
Deployment status: FAILED
Environment status: DEGRADED
Stored imageVersion: 0.2.1
```

The failed deployment keeps its failure reason in deployment history.

A later valid update can recover the environment:

```text
PATCH 0.2.2
    |
    v
DEPLOYING
    |
    v
SUCCESS
    |
    v
READY
```

After successful recovery:

```text
Environment status: READY
Stored imageVersion: 0.2.2
```

## Concurrent rollout protection

Only one rollout may be active for an environment at a time.

Before creating a deployment, EnvForge locks the environment row and
checks deployment history for an active deployment.

If another deployment is already `PENDING` or `IN_PROGRESS`, the new
request is rejected with:

```text
409 Conflict
```

The external Helm rollout executes after the short database claim
transaction has completed.

This prevents multiple Helm updates for the same environment from
running simultaneously.

## Authorization and deployment identity

Deployment operations are ownership-aware.

An environment owner with the appropriate operator permissions may
update the environment and inspect its deployment history.

A non-owner operator is rejected.

An administrator may access environments regardless of ownership.

The authenticated user is recorded as the deployment actor so the
deployment history provides an audit trail for version changes.

## Kubernetes security

Provisioned workloads use hardened Kubernetes security settings.

The deployment configuration includes protections such as:

```text
runAsNonRoot
allowPrivilegeEscalation: false
readOnlyRootFilesystem: true
seccompProfile
capabilities drop ALL
```

The charts also use fixed image tags and reject `latest`.

These checks are validated during automated deployment workflows.

## Local Kubernetes deployment

The functional reference environment for this repository is Kind.

The local deployment flow is:

```text
Control API
    |
    v
helm upgrade
    |
    v
Kind Kubernetes cluster
    |
    v
kubectl rollout status
```

Docker images used by local workflows are built locally and loaded
into the temporary Kind cluster.

The GitHub Actions `Update Environment` workflow validates the same
deployment path using an ephemeral Kind cluster.

## Update Environment workflow

The deployment workflow is defined in:

```text
.github/workflows/update-environment.yml
```

The workflow:

```text
creates a temporary Kind cluster
        |
        v
builds versioned workload images
        |
        v
loads images into Kind
        |
        v
starts PostgreSQL
        |
        v
starts the Control API
        |
        v
creates an environment
        |
        v
waits for READY
        |
        v
PATCHes the requested version
        |
        v
verifies Kubernetes rollout
        |
        v
verifies expected image
        |
        v
verifies environment state
        |
        v
verifies deployment history
```

This provides end-to-end validation of the deployment flow without
requiring a persistent Kubernetes environment.

## Ownership validation

The update workflow also validates deployment authorization.

A non-owner operator cannot update another user's environment.

Expected result:

```text
403 Forbidden
```

An owner operator may perform the update.

An administrator may inspect deployment history regardless of
environment ownership.

## Deployment actor

Deployment history records the authenticated actor responsible for an
update.

For example:

```text
triggeredBy: owner@envforge.local
```

This allows version changes to be traced back to the user who
initiated the deployment.

## Rollout concurrency

EnvForge serializes deployment updates for each environment.

The deployment claim is persisted before Helm begins the rollout.

A second request made while the first deployment is still active sees
the existing `IN_PROGRESS` deployment and is rejected.

Expected result:

```text
409 Conflict
```

The Helm command itself executes without holding the database
transaction open.

## Deployment metrics

EnvForge exposes deployment metrics through Micrometer.

Deployment metrics include rollout result and duration information.

These metrics allow Prometheus and Grafana to observe deployment
behavior such as:

```text
successful deployments
failed deployments
deployment duration
rollout failure rate
```

The observability stack contains deployment-oriented dashboard and
alerting configuration.

## Helm validation

The repository contains the following main Helm charts:

```text
deployment/helm/envforge-monitoring
deployment/helm/envforge-platform
deployment/helm/envforge-workload
deployment/helm/reliability-demo-api
```

Validate all charts with:

```bash
for chart in \
  deployment/helm/envforge-monitoring \
  deployment/helm/envforge-platform \
  deployment/helm/envforge-workload \
  deployment/helm/reliability-demo-api
do
  helm lint "${chart}"
done
```

Render a chart locally with:

```bash
helm template \
  example \
  deployment/helm/envforge-workload
```

## Immutable tag validation

A fixed version is accepted:

```bash
helm template \
  example \
  deployment/helm/envforge-workload \
  --set-string workload.image.tag=0.2.1
```

The following must fail:

```bash
helm template \
  example \
  deployment/helm/envforge-workload \
  --set-string workload.image.tag=latest
```

The reliability demo chart provides the equivalent protection through:

```text
image.tag
```

Therefore this must also fail:

```bash
helm template \
  reliability-example \
  deployment/helm/reliability-demo-api \
  --set-string image.tag=latest
```

## Docker validation

Important deployment images can be built locally before release.

Control API:

```bash
docker build \
  -f apps/control-api/Dockerfile \
  -t envforge/control-api:test \
  .
```

Static workload:

```bash
docker build \
  -t envforge/static-web-demo:test \
  apps/static-web-demo
```

Reliability demo:

```bash
docker build \
  -t envforge/reliability-demo-api:test \
  apps/reliability-demo-api
```

## API validation

Run the complete Control API test suite with PostgreSQL available:

```bash
cd apps/control-api
mvn test
```

Deployment-focused tests cover:

```text
version updates
rollout failures
failure recovery
deployment metrics
ownership authorization
authenticated deployment actor
concurrent rollout rejection
```

## Failure-state semantics

Provisioning failure and deployment failure intentionally have
different meanings.

Provisioning failure:

```text
REQUESTED
    |
    v
PROVISIONING
    |
    v
FAILED
```

Deployment update failure:

```text
READY
  |
  v
DEPLOYING
  |
  v
DEGRADED
```

In the second case the environment had previously reached a usable
state.

The deployment attempt itself is recorded as:

```text
FAILED
```

while the environment remains:

```text
DEGRADED
```

until a later successful update restores:

```text
READY
```

## Recovery example

Assume the environment currently runs:

```text
envforge/static-web-demo:0.2.0
```

The user requests:

```text
9.9.9
```

If the image does not exist, Kubernetes cannot complete the rollout.

EnvForge records:

```text
deployment:
  requestedVersion: 9.9.9
  status: FAILED

environment:
  status: DEGRADED
  imageVersion: 0.2.0
```

The stored version remains `0.2.0` because that was the last
successful application version.

The user may then request a valid version such as:

```text
0.2.1
```

After a successful rollout:

```text
deployment:
  requestedVersion: 0.2.1
  status: SUCCESS

environment:
  status: READY
  imageVersion: 0.2.1
```

## Azure infrastructure scope

The repository contains Terraform and deployment design for Azure
infrastructure including AKS and Azure Container Registry.

The Azure components demonstrate the intended cloud architecture as
infrastructure as code.

The fully validated functional Kubernetes environment documented in
this repository is the local Kind environment.

The repository should therefore not be interpreted as proof that a
live AKS or ACR environment is currently deployed.

## Release validation checklist

Before considering the deployment subsystem ready for a release,
validate:

```text
Control API tests pass
Docker images build
Helm charts lint successfully
Helm charts render successfully
latest image tags are rejected
successful update reaches READY
failed rollout reaches DEGRADED
previous image version is preserved after failure
valid recovery update returns the environment to READY
deployment history records SUCCESS and FAILED attempts
deployment actor is recorded
non-owner updates are rejected
concurrent rollouts are rejected
GitHub Actions deployment workflow passes
```

These checks provide coverage for the deployment lifecycle from API
request through Helm and Kubernetes rollout to deployment history,
failure handling and recovery.
