# Create an Environment

This guide explains how to create and inspect a temporary EnvForge environment through the portal or the Control API.

## Prerequisites

For local development, ensure that the following services are available:

- PostgreSQL;
- the `envforge` Kind cluster;
- the EnvForge Control API;
- the EnvForge portal;
- Helm and `kubectl`;
- the requested application image loaded into Kind.

Verify the local cluster:

```bash
kubectl cluster-info --context kind-envforge
helm version
```

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Start the Control API:

```bash
cd apps/control-api

ENVFORGE_KUBE_CONTEXT=kind-envforge \
ENVFORGE_HELM_CHART_PATH="$(
  realpath ../../deployment/helm/envforge-workload
)" \
./mvnw spring-boot:run
```

Start the portal in another terminal:

```bash
cd apps/portal
npm run dev
```

Open `http://localhost:5173`.

## Create through the portal

Complete the form with:

- an environment name containing lowercase letters, numbers and hyphens;
- an application template;
- an image version in `major.minor.patch` format;
- between one and five replicas;
- a resource profile;
- a lifetime between one and 24 hours;
- the monitoring option.

Example:

```text
Environment name: demo-web
Application template: Static Web App
Image version: 0.2.0
Replicas: 1
Resource profile: Small
Lifetime: 2 hours
Monitoring: enabled
```

Select **Create environment**.

During local provisioning, the create request can remain in the loading state while Helm waits for the Kubernetes Deployment. The default Helm timeout is two minutes.

## Environment states

A successful request moves through:

```text
REQUESTED → PROVISIONING → DEPLOYING → READY
```

A provisioning error produces:

```text
REQUESTED → PROVISIONING → DEPLOYING → FAILED
```

When the environment reaches `READY`, the portal displays:

- the Kubernetes namespace;
- the Helm release and its status;
- the Deployment;
- desired and ready replicas;
- the Service;
- the runtime observation time.

## Image version validation

Image versions must use `major.minor.patch` format.

Valid examples:

```text
0.1.0
1.4.2
10.20.30
```

Invalid examples:

```text
0..0
latest
1.2
1.2.3.4
```

Invalid versions are rejected before the environment is stored and before a Kubernetes namespace is created.

## Create through the API

```bash
curl --show-error \
  --fail-with-body \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "demo-web",
    "template": "STATIC_WEB",
    "imageVersion": "0.2.0",
    "replicas": 1,
    "resourceProfile": "SMALL",
    "lifetimeHours": 2,
    "monitoringEnabled": true
  }' \
  http://localhost:8080/api/environments \
  | jq .
```

A valid request returns `201 Created`. The response includes the environment ID, namespace and initial lifecycle status.

## Inspect an environment

List environments:

```bash
curl --silent http://localhost:8080/api/environments | jq .
```

Inspect one environment:

```bash
ENVFORGE_ENVIRONMENT_ID="replace-with-environment-id"

curl --silent \
  "http://localhost:8080/api/environments/${ENVFORGE_ENVIRONMENT_ID}" \
  | jq .
```

Inspect live Kubernetes and Helm state:

```bash
curl --silent \
  "http://localhost:8080/api/environments/${ENVFORGE_ENVIRONMENT_ID}/runtime" \
  | jq .
```

## Retry failed provisioning

Retry is available only when the environment status is `FAILED`.

Before retrying, resolve the original failure. For example, if the image is missing from Kind:

```bash
ENVFORGE_IMAGE_VERSION="0.2.0"

docker build \
  --tag "envforge/static-web-demo:${ENVFORGE_IMAGE_VERSION}" \
  apps/static-web-demo

kind load docker-image \
  "envforge/static-web-demo:${ENVFORGE_IMAGE_VERSION}" \
  --name envforge
```

The portal displays **Retry provisioning** for a failed environment.

The equivalent API request is:

```bash
ENVFORGE_ENVIRONMENT_ID="replace-with-environment-id"

curl --show-error \
  --fail-with-body \
  --request POST \
  "http://localhost:8080/api/environments/${ENVFORGE_ENVIRONMENT_ID}/retry" \
  | jq .
```

The accepted retry returns status `REQUESTED`. EnvForge then runs provisioning again and finishes with `READY` or `FAILED`.

Retrying an environment from any status other than `FAILED` returns `409 Conflict`.
