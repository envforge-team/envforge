# Runbook - High CPU Usage

## Trigger

Prometheus alert:

`EnvForgeReliabilityHighCpu`

Relevant recording rule:

`envforge_reliability:process_cpu_usage:avg`

The reliability alert is triggered when application CPU usage remains
above 0.80 for two minutes.

## Impact

High CPU usage can:

- increase application latency
- reduce request throughput
- cause readiness failures
- increase the risk of workload instability

## Initial Checks

Check the workload state:

```bash
kubectl get deployment,pods \
  -n env-reliability-demo \
  -o wide
```

Inspect recent application logs:

```bash
kubectl logs \
  -n env-reliability-demo \
  deployment/reliability-demo-api \
  --since=10m \
  --tail=200
```

Check recent Kubernetes events:

```bash
kubectl get events \
  -n env-reliability-demo \
  --sort-by=.lastTimestamp \
  | tail -30
```

## Check Prometheus CPU Metric

Start a temporary Prometheus tunnel:

```bash
kubectl port-forward \
  -n monitoring \
  svc/kube-prometheus-stack-prometheus \
  19090:9090
```

In another terminal, query the CPU recording rule:

```bash
curl -fsSG \
  http://localhost:19090/api/v1/query \
  --data-urlencode \
  'query=envforge_reliability:process_cpu_usage:avg'
```

Check the current alert state:

```bash
curl -fsS \
  http://localhost:19090/api/v1/alerts \
  | python3 -m json.tool \
  | grep -A12 EnvForgeReliabilityHighCpu
```

## Controlled CPU Incident

EnvForge contains a protected fault-injection endpoint for controlled
CPU load.

The maximum configured CPU load duration is:

`10000 ms`

Select one running reliability-demo-api pod:

```bash
POD="$(
  kubectl get pods \
    -n env-reliability-demo \
    -l app=reliability-demo-api \
    -o jsonpath='{.items[0].metadata.name}'
)"
```

Read the runtime incident key without printing it:

```bash
INCIDENT_KEY="$(
  kubectl get secret \
    reliability-demo-incident-admin \
    -n env-reliability-demo \
    -o jsonpath='{.data.ENVFORGE_INCIDENT_ADMIN_KEY}' \
    | base64 --decode
)"
```

Port-forward directly to the selected pod:

```bash
kubectl port-forward \
  -n env-reliability-demo \
  "pod/${POD}" \
  18080:8080
```

A controlled CPU test can be triggered from another terminal:

```bash
curl -fsS \
  -X POST \
  -H "X-EnvForge-Incident-Key: ${INCIDENT_KEY}" \
  'http://localhost:18080/admin/incidents/cpu?milliseconds=10000'
```

Do not print or commit the incident administration key.

## Persistent CPU Usage

If CPU remains elevated after a controlled test has finished:

1. inspect application logs
2. inspect pod restart counts
3. inspect Kubernetes events
4. confirm whether request traffic is unusually high
5. identify whether one replica or all replicas are affected

Avoid restarting workloads before collecting diagnostic information.

## Pod-Level Investigation

List pod restart counts:

```bash
kubectl get pods \
  -n env-reliability-demo \
  -l app=reliability-demo-api
```

Inspect an affected pod:

```bash
kubectl describe pod \
  -n env-reliability-demo \
  <pod-name>
```

Check its logs:

```bash
kubectl logs \
  -n env-reliability-demo \
  <pod-name> \
  --tail=200
```

## Recovery

If one pod remains unhealthy after diagnostic information has been
collected, recreate only that pod:

```bash
kubectl delete pod \
  -n env-reliability-demo \
  <pod-name>
```

The Deployment should automatically create a replacement.

Avoid restarting the entire Deployment unless multiple replicas require
recovery.

## Recovery Validation

Query CPU again:

```bash
curl -fsSG \
  http://localhost:19090/api/v1/query \
  --data-urlencode \
  'query=envforge_reliability:process_cpu_usage:avg'
```

Confirm workload availability:

```bash
kubectl wait \
  deployment/reliability-demo-api \
  -n env-reliability-demo \
  --for=condition=available \
  --timeout=120s
```

## Final Validation

Run:

```bash
ENVFORGE_KUBE_CONTEXT="$(kubectl config current-context)" \
./observability/scripts/validate-kind-observability.sh
```

Expected result:

`EnvForge observability validation PASS`
