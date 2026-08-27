# Day 39 - Kind Monitoring Demo

## Goal

Demonstrate the EnvForge monitoring and reliability stack running on the
local Kind Kubernetes environment.

The Day 39 demo is performed entirely on Kind.

No Azure deployment is required for this validation.

## Demo Environment

The monitoring demo uses the local Kubernetes cluster and the following
components:

- reliability-demo-api
- traffic-generator
- Prometheus
- Prometheus recording rules
- Prometheus alert rules
- Grafana
- Loki
- Alloy

## Monitoring Flow

Application metrics follow this path:

```text
reliability-demo-api
        |
        v
Micrometer / Actuator
        |
        v
ServiceMonitor
        |
        v
Prometheus
        |
        +--> Recording Rules
        |
        +--> Alert Rules
        |
        v
Grafana
```

Application and traffic-generator logs follow:

```text
Container Logs
      |
      v
Alloy
      |
      v
Loki
      |
      v
Grafana
```

## Grafana Demo

Grafana is exposed locally with:

```bash
kubectl port-forward \
  -n monitoring \
  svc/kube-prometheus-stack-grafana \
  3000:80
```

The Grafana interface is then available at:

`http://localhost:3000`

The demo uses the:

`EnvForge Reliability`

dashboard.

The dashboard contains panels for:

- reliability targets
- request rate
- HTTP 5xx ratio
- average HTTP latency
- HTTP request and 5xx rates
- application CPU usage
- JVM memory usage
- HTTP latency over time
- target availability over time
- reliability workload logs

## Prometheus Demo

Prometheus is exposed locally with:

```bash
kubectl port-forward \
  -n monitoring \
  svc/kube-prometheus-stack-prometheus \
  9090:9090
```

The Prometheus interface is available at:

`http://localhost:9090`

The demo confirms that the reliability workload is actively scraped by
Prometheus.

## Recording Rules

The following EnvForge recording rules are available:

```text
envforge_reliability:http_requests_per_second:rate5m
envforge_reliability:http_5xx_per_second:rate5m
envforge_reliability:http_5xx_ratio:rate5m
envforge_reliability:http_avg_latency_seconds:rate5m
envforge_reliability:targets_up
envforge_reliability:process_cpu_usage:avg
envforge_reliability:jvm_memory_used_bytes:sum
```

The target availability recording rule reports the expected healthy
reliability workload targets.

## Alert Rules

The following Prometheus alerts are loaded:

```text
EnvForgeReliabilityTargetDown
EnvForgeReliabilityHigh5xxRatio
EnvForgeReliabilityHighLatency
EnvForgeReliabilityHighCpu
```

The alert rules are healthy and can be inspected directly through the
Prometheus alert interface and API.

## Traffic Demo

The reliability workload can be accessed locally with:

```bash
kubectl port-forward \
  -n env-reliability-demo \
  svc/reliability-demo-api \
  18080:80
```

Example traffic generation:

```bash
for i in $(seq 1 30); do
  curl -fsS http://localhost:18080/work >/dev/null
done
```

This produces measurable workload activity visible through Prometheus
and Grafana.

The continuously running traffic-generator also maintains background
traffic for the monitoring environment.

## Log Demo

Traffic-generator logs can be inspected directly with:

```bash
kubectl logs \
  -n env-reliability-demo \
  deployment/traffic-generator \
  --tail=20
```

The same workload logging pipeline is visible in the Grafana reliability
dashboard through Loki.

This demonstrates the complete logging flow:

```text
workload
-> container logs
-> Alloy
-> Loki
-> Grafana
```

## Reliability Demo

The monitoring environment also supports controlled reliability
incidents through the protected reliability-demo-api administration
endpoints.

Previous validation confirms the complete incident lifecycle:

```text
controlled failure
-> HTTP 5xx metrics
-> Prometheus recording rule
-> alert pending
-> alert firing
-> incident reset
-> workload recovery
-> alert resolution
```

The administration key used by the controlled incident endpoints remains
runtime-only and is not stored in the repository.

## Monitoring Validation

The complete monitoring environment is validated with:

```bash
ENVFORGE_KUBE_CONTEXT="$(kubectl config current-context)" \
./observability/scripts/validate-kind-observability.sh
```

The validation confirms:

- reliability workload health
- workload readiness
- raw Micrometer metrics
- Prometheus targets
- recording rules
- alert rules
- Loki log ingestion
- Grafana datasources
- Grafana dashboard
- traffic-generator availability

The Day 39 validation completed successfully.

## Platform Choice

The functional EnvForge Kubernetes environment is Kind.

The monitoring and reliability demo therefore uses the same functional
Kind environment rather than depending on an external cloud deployment.

This keeps the monitoring demonstration reproducible on a developer
machine while exercising the real Kubernetes, Prometheus, Grafana, Loki
and Alloy integrations.

## Result

Day 39 demonstrates a complete working observability path on Kind:

```text
Metrics:
Application
-> Micrometer
-> Prometheus
-> Recording Rules
-> Alerts
-> Grafana

Logs:
Application / Traffic Generator
-> Alloy
-> Loki
-> Grafana
```

The EnvForge monitoring stack is operational and ready for the final
dashboard export and contribution summary.
