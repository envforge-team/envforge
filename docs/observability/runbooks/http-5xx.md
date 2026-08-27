# Runbook - High HTTP 5xx Rate

## Trigger

Prometheus alert:

`EnvForgeReliabilityHigh5xxRatio`

The alert is configured when the reliability workload 5xx ratio exceeds
5 percent for one minute.

Relevant recording rule:

`envforge_reliability:http_5xx_ratio:rate5m`

## Impact

Users may receive HTTP 5xx responses from the reliability workload.

The monitoring stack must remain available so that metrics and logs can
be used during diagnosis.

## Initial Checks

Check the workload:

```bash
kubectl get deployment,pods \
  -n env-reliability-demo \
  -o wide
```

Inspect recent logs:

```bash
kubectl logs \
  -n env-reliability-demo \
  deployment/reliability-demo-api \
  --since=10m \
  --tail=200
```

Inspect recent Kubernetes events:

```bash
kubectl get events \
  -n env-reliability-demo \
  --sort-by=.lastTimestamp \
  | tail -30
```

## Check Prometheus

Start a temporary tunnel:

```bash
kubectl port-forward \
  -n monitoring \
  svc/kube-prometheus-stack-prometheus \
  19090:9090
```

In another terminal:

```bash
curl -fsSG \
  http://localhost:19090/api/v1/query \
  --data-urlencode \
  'query=envforge_reliability:http_5xx_ratio:rate5m'
```

Check the alert:

```bash
curl -fsS \
  http://localhost:19090/api/v1/alerts \
  | python3 -m json.tool \
  | grep -A12 EnvForgeReliabilityHigh5xxRatio
```

## Check Controlled Incident State

Select one running reliability pod:

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

In another terminal where `INCIDENT_KEY` is available:

```bash
curl -fsS \
  -H "X-EnvForge-Incident-Key: ${INCIDENT_KEY}" \
  http://localhost:18080/admin/incidents/status
```

If a controlled failure incident is enabled unexpectedly, reset it:

```bash
curl -fsS \
  -X POST \
  -H "X-EnvForge-Incident-Key: ${INCIDENT_KEY}" \
  http://localhost:18080/admin/incidents/reset
```

Do not print or commit the incident administration key.

## Recovery Validation

Verify workload availability:

```bash
kubectl wait \
  deployment/reliability-demo-api \
  -n env-reliability-demo \
  --for=condition=available \
  --timeout=120s
```

Verify the workload endpoint:

```bash
kubectl port-forward \
  -n env-reliability-demo \
  svc/reliability-demo-api \
  18081:80
```

In another terminal:

```bash
curl -i http://localhost:18081/work
```

Expected:

`HTTP 200`

Because the alert uses a five-minute rate window, the alert may remain
pending or firing for several minutes after the workload recovers.

## Final Validation

Run:

```bash
ENVFORGE_KUBE_CONTEXT="$(kubectl config current-context)" \
./observability/scripts/validate-kind-observability.sh
```

Expected result:

`EnvForge observability validation PASS`
