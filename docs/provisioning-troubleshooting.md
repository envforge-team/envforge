# Provisioning Troubleshooting

This runbook covers common failures in the EnvForge environment creation and retry flow.

## Quick diagnostic checklist

Set the environment details:

```bash
ENVFORGE_NAME="replace-with-environment-name"
ENVFORGE_NAMESPACE="env-${ENVFORGE_NAME}"
```

Inspect the API state:

```bash
curl --silent \
  http://localhost:8080/api/environments \
  | jq \
      --arg name "$ENVFORGE_NAME" \
      '.[] | select(.name == $name)'
```

Inspect Kubernetes resources:

```bash
kubectl get all --namespace "$ENVFORGE_NAMESPACE"

kubectl get events \
  --namespace "$ENVFORGE_NAMESPACE" \
  --sort-by=.lastTimestamp \
  | tail -n 40
```

Inspect Helm:

```bash
helm status \
  "$ENVFORGE_NAME" \
  --namespace "$ENVFORGE_NAMESPACE"

helm history \
  "$ENVFORGE_NAME" \
  --namespace "$ENVFORGE_NAMESPACE"
```

## ImagePullBackOff

Typical symptoms are `ErrImagePull`, `ImagePullBackOff` or `pull access denied`.

Verify the image used by the Deployment:

```bash
kubectl get deployment \
  "${ENVFORGE_NAME}-envforge-workload" \
  --namespace "$ENVFORGE_NAMESPACE" \
  --output jsonpath='{.spec.template.spec.containers[0].image}'

echo
```

Verify images loaded into Kind:

```bash
docker exec \
  envforge-control-plane \
  crictl images \
  | grep static-web-demo
```

Build and load the missing version:

```bash
ENVFORGE_IMAGE_VERSION="replace-with-version"

docker build \
  --tag "envforge/static-web-demo:${ENVFORGE_IMAGE_VERSION}" \
  apps/static-web-demo

kind load docker-image \
  "envforge/static-web-demo:${ENVFORGE_IMAGE_VERSION}" \
  --name envforge
```

Restart the Deployment and wait for it:

```bash
kubectl rollout restart \
  "deployment/${ENVFORGE_NAME}-envforge-workload" \
  --namespace "$ENVFORGE_NAMESPACE"

kubectl rollout status \
  "deployment/${ENVFORGE_NAME}-envforge-workload" \
  --namespace "$ENVFORGE_NAMESPACE" \
  --timeout 60s
```

Retry provisioning only after the Deployment can become healthy.

## Progress deadline exceeded

Helm can report:

```text
Progress deadline exceeded
```

This means that the Deployment did not become ready before its progress deadline.

Inspect it:

```bash
kubectl describe deployment \
  "${ENVFORGE_NAME}-envforge-workload" \
  --namespace "$ENVFORGE_NAMESPACE"

kubectl get pods \
  --namespace "$ENVFORGE_NAMESPACE" \
  --output wide
```

Resolve the underlying pod failure, wait for a successful rollout, then retry the failed environment.

## Helm release remains failed

A Kubernetes Deployment can become healthy after Helm has already timed out. In this situation:

- the pod can show `Running`;
- replicas can show `1/1`;
- the Helm release can still show `failed`;
- the EnvForge environment remains `FAILED`.

Wait for the Deployment:

```bash
kubectl rollout status \
  "deployment/${ENVFORGE_NAME}-envforge-workload" \
  --namespace "$ENVFORGE_NAMESPACE" \
  --timeout 60s
```

Then use the EnvForge retry action. A successful retry creates a new Helm revision with status `deployed`.

## Invalid image version

Invalid versions such as `0..0` are rejected with `400 Bad Request`.

Expected validation response:

```json
{
  "status": 400,
  "message": "Request validation failed",
  "validationErrors": {
    "imageVersion": "Image version must follow semantic versioning (e.g. 1.4.2)"
  }
}
```

No environment record or namespace should be created.

## Retry returns 409 Conflict

Retry is allowed only from `FAILED`.

Inspect the current status:

```bash
ENVFORGE_ENVIRONMENT_ID="replace-with-environment-id"

curl --silent \
  "http://localhost:8080/api/environments/${ENVFORGE_ENVIRONMENT_ID}" \
  | jq '{id, name, status}'
```

Do not retry environments that are already provisioning, ready, deleting, deleted or expired.

## Runtime card reports attention required

Inspect the runtime endpoint:

```bash
curl --silent \
  "http://localhost:8080/api/environments/${ENVFORGE_ENVIRONMENT_ID}/runtime" \
  | jq .
```

A healthy local environment requires:

- `namespaceExists` equal to `true`;
- `helmStatus` equal to `deployed`;
- a non-null Deployment;
- a non-null Service;
- equal desired and ready replica counts.

## Control API cannot execute Helm or kubectl

Verify the configured context and chart:

```bash
kubectl config get-contexts
kubectl cluster-info --context kind-envforge
helm lint deployment/helm/envforge-workload
```

Restart the API with explicit local settings:

```bash
cd apps/control-api

ENVFORGE_KUBE_CONTEXT=kind-envforge \
ENVFORGE_HELM_CHART_PATH="$(
  realpath ../../deployment/helm/envforge-workload
)" \
./mvnw spring-boot:run
```

## Logs to collect

When escalating a provisioning problem, include:

```bash
kubectl describe deployment \
  "${ENVFORGE_NAME}-envforge-workload" \
  --namespace "$ENVFORGE_NAMESPACE"

kubectl get events \
  --namespace "$ENVFORGE_NAMESPACE" \
  --sort-by=.lastTimestamp \
  | tail -n 40

helm status \
  "$ENVFORGE_NAME" \
  --namespace "$ENVFORGE_NAMESPACE"

helm history \
  "$ENVFORGE_NAME" \
  --namespace "$ENVFORGE_NAMESPACE"
```

Also include the exception beginning with `Provisioning failed for environment` from the Control API logs.
