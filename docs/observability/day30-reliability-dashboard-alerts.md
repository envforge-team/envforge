# Day 30 - Reliability Dashboard and Alerts

## Goal

Finalize the Grafana reliability dashboard and Prometheus alert rules for
the EnvForge monitoring workflow.

## Grafana Dashboard

Dashboard:

`EnvForge Reliability`

UID:

`envforge-reliability`

The dashboard is stored as:

`observability/dashboards/envforge-reliability.json`

and provisioned in the local Kind environment through:

`observability/kubernetes/reliability-dashboard-configmap.yaml`

## Dashboard Panels

The dashboard contains ten panels:

1. Reliability targets UP
2. Request rate
3. HTTP 5xx ratio
4. Average HTTP latency
5. HTTP request and 5xx rate
6. Application CPU usage
7. JVM memory usage
8. HTTP latency over time
9. Target availability over time
10. Reliability workload logs

Prometheus recording rules provide the metrics used by the dashboard.

Loki provides workload logs.

## Reliability Alerts

PrometheusRule:

`envforge-reliability-alert-rules`

Configured alerts:

- EnvForgeReliabilityTargetDown
- EnvForgeReliabilityHigh5xxRatio
- EnvForgeReliabilityHighLatency
- EnvForgeReliabilityHighCpu

The rules cover:

- workload target availability
- excessive HTTP 5xx responses
- high HTTP latency
- high application CPU usage

Alert firing behavior is exercised later by the end-to-end incident and
alerting test.

## CI Validation

The observability validation workflow installs:

- recording rules
- reliability alert rules
- reliability dashboard ConfigMap

The end-to-end validation checks that Grafana loads the reliability
dashboard and that the required telemetry path remains functional.

## Azure Monitor

The original project plan also included Azure Monitor queries.

The functional environment currently uses Kind with Prometheus, Grafana,
Loki and Alloy.

Azure monitoring resources are therefore not required for the local
functional validation and no Terraform apply is performed.

## Result

The local EnvForge observability stack now provides:

Workload metrics
→ Prometheus
→ recording rules
→ reliability dashboard
→ alert rules

and:

Workload logs
→ Alloy
→ Loki
→ Grafana
