# Runbook - Monitoring Stack Unavailable

## Trigger

One or more monitoring components are unavailable:

- Prometheus
- Grafana
- Loki
- Alloy

The EnvForge workload should continue operating while monitoring is
degraded.

## Immediate Platform Check

First confirm that the application itself is still operational:

```bash
kubectl get deployment,pods \
  -n env-reliability-demo \
  -o wide
```

Check the workload directly:

```bash
kubectl port-forward \
  -n env-reliability-demo \
  svc/reliability-demo-api \
  18080:80
```

In another terminal:

```bash
curl -i http://localhost:18080/work
```

Expected during a monitoring-only outage:

`HTTP 200`

## Check Monitoring Namespace

List monitoring pods:

```bash
kubectl get pods \
  -n monitoring \
  -o wide
```

List deployments and StatefulSets:

```bash
kubectl get deployment,statefulset \
  -n monitoring
```

List monitoring Services:

```bash
kubectl get svc \
  -n monitoring
```

Inspect recent events:

```bash
kubectl get events \
  -n monitoring \
  --sort-by=.lastTimestamp \
  | tail -40
```

## Prometheus

Check the Prometheus StatefulSet:

```bash
kubectl get statefulset \
  prometheus-kube-prometheus-stack-prometheus \
  -n monitoring
```

Check the Prometheus Operator:

```bash
kubectl get deployment \
  kube-prometheus-stack-operator \
  -n monitoring
```

If Prometheus is running, verify readiness:

```bash
kubectl port-forward \
  -n monitoring \
  svc/kube-prometheus-stack-prometheus \
  19090:9090
```

In another terminal:

```bash
curl -i http://localhost:19090/-/ready
```

Expected:

`HTTP 200`

## Grafana

Check the Grafana Deployment:

```bash
kubectl get deployment \
  kube-prometheus-stack-grafana \
  -n monitoring
```

Inspect Grafana logs:

```bash
kubectl logs \
  -n monitoring \
  deployment/kube-prometheus-stack-grafana \
  --since=10m \
  --tail=200
```

## Loki and Alloy

Discover Loki and Alloy resources:

```bash
kubectl get pods,svc \
  -n monitoring \
  | grep -E 'loki|alloy'
```

Inspect an unhealthy pod:

```bash
kubectl describe pod \
  -n monitoring \
  <pod-name>
```

Inspect logs:

```bash
kubectl logs \
  -n monitoring \
  <pod-name> \
  --since=10m \
  --tail=200
```

## Restore Monitoring Configuration

If monitoring resources were accidentally removed or left scaled down,
reconcile the stack from the repository configuration.

### Prometheus and Grafana

```bash
helm upgrade --install \
  kube-prometheus-stack \
  prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --version 87.21.0 \
  -f observability/kubernetes/kube-prometheus-stack-kind-values.yaml
```

### Loki

```bash
helm upgrade --install \
  loki \
  grafana/loki \
  --namespace monitoring \
  --version 18.9.0 \
  -f observability/kubernetes/loki-kind-values.yaml
```

### Alloy

```bash
helm upgrade --install \
  alloy \
  grafana/alloy \
  --namespace monitoring \
  --version 1.11.1 \
  -f observability/kubernetes/alloy-kind-values.yaml
```

## Validate Monitoring Recovery

Watch monitoring pods:

```bash
kubectl get pods \
  -n monitoring \
  -w
```

Stop the watch with `Ctrl+C` after the components become healthy.

Then run the complete observability validation:

```bash
ENVFORGE_KUBE_CONTEXT="$(kubectl config current-context)" \
./observability/scripts/validate-kind-observability.sh
```

Expected result:

`EnvForge observability validation PASS`

## Validate Platform Resilience

EnvForge also provides a dedicated monitoring outage test:

```bash
ENVFORGE_KUBE_CONTEXT="$(kubectl config current-context)" \
./observability/scripts/validate-monitoring-outage.sh
```

This validates that Prometheus and Grafana can become unavailable
without blocking the reliability workload.

Expected result:

`PASS: Prometheus/Grafana outage does not block the reliability workload.`

## Escalation

Escalate the incident if:

- monitoring cannot be restored from the repository configuration
- Prometheus repeatedly fails after restart
- Grafana cannot reach its datasources
- Loki ingestion does not recover
- Alloy cannot resume log forwarding
- the application workload is also unavailable

If the workload itself is affected, use the workload-specific runbooks
instead of treating the incident as monitoring-only.

## Result

A monitoring outage should degrade observability capabilities without
blocking the EnvForge application workload.
