# Environment Deployment Flow

## Purpose

The deployment flow updates an existing environment to a newer application version.

## Input

The operator provides:

- environment id;
- target application version.

## Flow

```mermaid
sequenceDiagram
    actor Operator
    participant Portal
    participant API as Control API
    participant DB as PostgreSQL
    participant ACR as Azure Container Registry
    participant Helm
    participant AKS

    Operator->>Portal: Select new version
    Portal->>API: PATCH /api/environments/{id}
    API->>DB: Validate environment
    API->>ACR: Verify image exists
    API->>Helm: helm upgrade
    Helm->>AKS: Update deployment
    AKS-->>Helm: Rollout status
    Helm->>API: Deployment result
    API->>DB: Save deployment history
    API-->>Portal: Return updated status
```

## Generated values

Example request:

```json
{
  "environmentId": 1,
  "version": "1.1.0"
}
```

Generated values:

```text
Deployment revision: 5
Status: DEPLOYING
```

## Failure scenarios

- environment not found;
- invalid version;
- image not found in ACR;
- helm upgrade failed;
- rollout timeout;
- deployment cancelled.