# Day 37 - Monitoring Verification

## Goal

Perform a complete verification of the EnvForge monitoring stack,
including workload health, metrics, recording rules, alerts, logs and
Grafana dashboards.

## Workload Health

The reliability workload validation confirms:

- reliability-demo-api health endpoint is reachable
- reliability-demo-api readiness endpoint is reachable
- reliability-demo-api deployment is available
- traffic-generator deployment is available

## Raw Metrics

The workload exposes Prometheus metrics through:

`/actuator/prometheus`

The validation confirms metrics covering:

- HTTP requests
- process CPU usage
- JVM memory usage

## Prometheus Targets

Prometheus successfully discovers the reliability-demo-api metrics
targets.

The expected workload targets are confirmed as UP.

## Recording Rules

All seven EnvForge reliability recording rules are loaded and healthy:

- envforge_reliability:http_requests_per_second:rate5m
- envforge_reliability:http_5xx_per_second:rate5m
- envforge_reliability:http_5xx_ratio:rate5m
- envforge_reliability:http_avg_latency_seconds:rate5m
- envforge_reliability:targets_up
- envforge_reliability:process_cpu_usage:avg
- envforge_reliability:jvm_memory_used_bytes:sum

## Alert Rules

All four reliability alerts are loaded and healthy:

- EnvForgeReliabilityTargetDown
- EnvForgeReliabilityHigh5xxRatio
- EnvForgeReliabilityHighLatency
- EnvForgeReliabilityHighCpu

## Logs

Loki successfully contains logs produced by the traffic-generator.

This confirms that the workload logging pipeline remains operational.

## Grafana Datasources

Grafana contains and can reach both required datasources:

- Prometheus
- Loki

## Reliability Dashboard

The EnvForge Reliability dashboard is available in Grafana.

Validation confirms all ten required panels covering:

- target availability
- request rate
- HTTP 5xx ratio
- HTTP latency
- request and 5xx rates over time
- CPU usage
- JVM memory usage
- latency over time
- target availability over time
- workload logs

## Automated Validation

Day 37 extends:

`observability/scripts/validate-kind-observability.sh`

The validator verifies the complete monitoring path rather than only
checking that Kubernetes resources exist.

## Result

The EnvForge reliability monitoring stack successfully passes
verification for:

health
-> metrics
-> Prometheus targets
-> recording rules
-> alerts
-> Loki logs
-> Grafana datasources
-> Grafana dashboard
