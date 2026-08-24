# Local end-to-end provisioning test

This runbook verifies the complete local EnvForge M1 provisioning flow:

```text
Portal or curl
→ Control API
→ PostgreSQL
→ provisioning event
→ Kubernetes namespace
→ Helm release
→ workload Deployment and Service
→ Micrometer metrics
→ Prometheus
→ Grafana
```

## Prerequisites

The following tools must be available:

- Docker Desktop with WSL integration;
- Docker Compose;
- Java 21;
- Kind;
- kubectl;
- Helm;
- curl;
- jq.

The local Kubernetes cluster must use the following context:

```text
kind-envforge
```

## Start the local infrastructure

From the repository root, determine the current WSL address:

```bash
export WSL_HOST_IP="$(
  ip -4 addr show eth0 \
  | awk '/inet / {
      split($2, address, "/")
      print address[1]
    }'
)"

echo "WSL_HOST_IP=$WSL_HOST_IP"
```

Start PostgreSQL, Prometheus and Grafana:

```bash
docker compose up \
  -d \
  postgres \
  prometheus \
  grafana

docker compose ps
```

## Verify the Kind cluster

```bash
kind get clusters

kubectl config use-context kind-envforge

kubectl get nodes
```

The EnvForge node must report the `Ready` status.

## Start Control API

Run the application in a separate terminal:

```bash
cd apps/control-api

ENVFORGE_KUBE_CONTEXT=kind-envforge \
ENVFORGE_HELM_CHART_PATH="$(
  realpath ../../deployment/helm/envforge-workload
)" \
./mvnw spring-boot:run
```

Keep this terminal open.

## Verify local services

From another terminal:

```bash
curl --fail-with-body \
  http://localhost:8080/actuator/health

curl --fail-with-body \
  http://localhost:9090/-/ready
```

Control API must report `UP`, and Prometheus must report that it is ready.

## Run the automated end-to-end test

From the repository root:

```bash
./scripts/smoke-test-local.sh
```

The script verifies:

1. required command-line tools;
2. active Kind context;
3. Kubernetes node readiness;
4. Control API health;
5. environment template availability;
6. Prometheus readiness;
7. creation of a unique environment;
8. transition from `REQUESTED` or `PROVISIONING` to `READY`;
9. environment persistence and API retrieval;
10. managed Kubernetes namespace;
11. Helm release status;
12. workload Deployment availability;
13. Service, ResourceQuota and LimitRange resources;
14. successful provisioning metric in Prometheus.

## Expected result

```text
Local end-to-end test passed.
Environment ID:        <generated UUID>
Environment name:      e2e-m1-<timestamp>
Namespace:             env-e2e-m1-<timestamp>
Status:                READY
Kubernetes context:    kind-envforge
Image version:         0.2.0
Prometheus validation: passed
```

## Grafana verification

Open:

```text
http://localhost:3000/d/envforge-provisioning
```

The dashboard should display:

- successful provisioning attempts;
- failed provisioning attempts;
- average provisioning duration;
- maximum provisioning duration;
- provisioning attempts grouped by outcome and template.

Default local credentials:

```text
Username: admin
Password: envforge-local-admin
```

## Environment cleanup

The created environment intentionally remains available after the test so that
its API, Helm and monitoring state can be inspected.

Normal cleanup belongs to the EnvForge lifecycle flow. Avoid manually deleting
only the Kubernetes namespace because that would leave the database record out
of sync with the cluster.

## Troubleshooting

### Control API is unavailable

```bash
ss -ltnp | grep ':8080'

curl http://localhost:8080/actuator/health
```

### Prometheus target is down

Recalculate the WSL address and recreate Prometheus:

```bash
export WSL_HOST_IP="$(
  ip -4 addr show eth0 \
  | awk '/inet / {
      split($2, address, "/")
      print address[1]
    }'
)"

docker compose up \
  -d \
  --force-recreate \
  prometheus
```

Verify:

```bash
curl --silent \
  http://localhost:9090/api/v1/targets \
  | jq .
```

### Wrong Kubernetes context

```bash
kubectl config use-context kind-envforge
kubectl get nodes
```

### Helm provisioning failed

```bash
helm list --all-namespaces

kubectl get events \
  --namespace <environment-namespace> \
  --sort-by=.lastTimestamp
```

### Workload image is unavailable in Kind

Build and load the local image:

```bash
docker build \
  --tag envforge/static-web-demo:0.2.0 \
  apps/static-web-demo

kind load docker-image \
  envforge/static-web-demo:0.2.0 \
  --name envforge
```

### PostgreSQL is unavailable

```bash
docker compose ps postgres
docker compose logs postgres
```