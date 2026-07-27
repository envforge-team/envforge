# Environment Provisioning Flow

## Purpose

The provisioning flow creates an isolated temporary Kubernetes environment from a request submitted through the EnvForge portal.

## Input

The user provides:

- environment name;
- application template;
- image version;
- replicas;
- resource profile;
- lifetime;
- monitoring preference.

## Flow

```mermaid
sequenceDiagram
    actor User
    participant Portal
    participant API as Control API
    participant DB as PostgreSQL
    participant Workflow as GitHub Actions
    participant AKS
    participant Helm

    User->>Portal: Complete environment form
    Portal->>API: POST /api/environments
    API->>API: Validate request
    API->>DB: Save REQUESTED environment
    API->>Workflow: Trigger provisioning
    Workflow->>AKS: Create namespace
    Workflow->>Helm: Install release
    Helm->>AKS: Create workload resources
    Workflow->>AKS: Verify rollout
    Workflow->>API: Report result
    API->>DB: Save READY or FAILED
    Portal->>API: Request environment status
    API-->>Portal: Return current status
```

## Generated values

Example request:

```json
{
  "name": "static-demo-bogdan",
  "template": "STATIC_WEB",
  "imageVersion": "0.1.0",
  "replicas": 2,
  "resourceProfile": "SMALL",
  "lifetimeHours": 4,
  "monitoringEnabled": true
}
```

Generated values:

```text
Namespace: env-static-demo-bogdan
Helm release: static-demo-bogdan
Status: REQUESTED
```

## Failure scenarios

- invalid environment name;
- duplicate environment name;
- image does not exist;
- namespace already exists;
- Helm installation fails;
- rollout times out;
- smoke test fails;
- status callback fails.