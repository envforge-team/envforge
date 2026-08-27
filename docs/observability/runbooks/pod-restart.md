# Runbook - Unexpected Pod Restart

## Trigger

A `reliability-demo-api` pod shows an increased restart count, enters a
crash loop, or is repeatedly recreated.

## Impact

A single pod restart should not cause a complete workload outage because
the reliability demo runs multiple replicas.

Repeated restarts may:

- reduce workload availability
- remove Prometheus scrape targets temporarily
- increase application latency
- indicate probe failures, OOM events or application crashes

## Initial Checks

List the reliability workload:

```bash
kubectl get deployment,pods \
  -n env-reliability-demo \
  -o wide
```

List only the application pods and their restart counts:

```bash
kubectl get pods \
  -n env-reliability-demo \
  -l app=reliability-demo-api
```

Identify the affected pod:

```bash
POD=<affected-pod-name>
```

## Inspect the Pod

Describe the pod:

```bash
kubectl describe pod \
  -n env-reliability-demo \
  "${POD}"
```

Look for:

- `OOMKilled`
- failed liveness probes
- failed readiness probes
- container exit codes
- scheduling failures
- node problems
- image pull failures

## Inspect Logs

Current container logs:

```bash
kubectl logs \
  -n env-reliability-demo \
  "${POD}" \
  --tail=200
```

If the container restarted, inspect logs from the previous instance:

```bash
kubectl logs \
  -n env-reliability-demo \
  "${POD}" \
  --previous \
  --tail=200
```

## Kubernetes Events

Inspect recent namespace events:

```bash
kubectl get events \
  -n env-reliability-demo \
  --sort-by=.lastTimestamp \
  | tail -40
```

Events can help identify:

- container crashes
- probe failures
- OOM conditions
- failed scheduling
- node issues
- image errors

## Deployment Status

Check rollout state:

```bash
kubectl rollout status \
  deployment/reliability-demo-api \
  -n env-reliability-demo \
  --timeout=120s
```

Check the desired and available replica counts:

```bash
kubectl get deployment reliability-demo-api \
  -n env-reliability-demo
```

The normal reliability demo deployment should have its expected replicas
available after recovery.

## Prometheus Target Check

Start a temporary Prometheus tunnel:

```bash
kubectl port-forward \
  -n monitoring \
  svc/kube-prometheus-stack-prometheus \
  19090:9090
```

In another terminal, check the target recording rule:

```bash
curl -fsSG \
  http://localhost:19090/api/v1/query \
  --data-urlencode \
  'query=envforge_reliability:targets_up'
```

For the current reliability demo deployment, the healthy expected value
is:

`2`

A temporary value below `2` may occur while a pod is being replaced.

## Recovery

If the affected pod remains unhealthy after diagnostic information has
been collected, delete only that pod:

```bash
kubectl delete pod \
  -n env-reliability-demo \
  "${POD}"
```

The Deployment controller should automatically create a replacement.

Watch the replacement pod:

```bash
kubectl get pods \
  -n env-reliability-demo \
  -l app=reliability-demo-api \
  -w
```

Stop the watch with `Ctrl+C` after the replacement becomes `Running`
and ready.

## Verify Application Recovery

Wait for the Deployment:

```bash
kubectl wait \
  deployment/reliability-demo-api \
  -n env-reliability-demo \
  --for=condition=available \
  --timeout=120s
```

Check the workload endpoint:

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

## Verify Prometheus Recovery

Query the target recording rule again:

```bash
curl -fsSG \
  http://localhost:19090/api/v1/query \
  --data-urlencode \
  'query=envforge_reliability:targets_up'
```

Expected after both replicas recover:

`2`

## Final Validation

Run:

```bash
ENVFORGE_KUBE_CONTEXT="$(kubectl config current-context)" \
./observability/scripts/validate-kind-observability.sh
```

Expected result:

`EnvForge observability validation PASS`
