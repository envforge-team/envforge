# Day 25 - Kind Monitoring Integration

## Goal

Validate the complete EnvForge observability integration in the functional
local Kubernetes environment.

The original cloud-oriented monitoring integration is represented locally
using Kind.

## Functional Environment

Kubernetes:

`kind-envforge-local`

Monitoring namespace:

`monitoring`

Application namespace:

`env-reliability-demo`

## Metrics Flow

Validated path:

Workload
→ Spring Boot Actuator / Micrometer
→ Kubernetes Service
→ ServiceMonitor
→ Prometheus
→ Grafana

The reliability demo exposes Prometheus metrics through:

`/actuator/prometheus`

Prometheus discovers both workload replicas through the
`reliability-demo-api-metrics` Service and ServiceMonitor.

Validation checks confirm:

- both workload replicas are available
- metrics endpoints are discovered
- Prometheus targets report `up = 1`
- `/work` HTTP request metrics are present
- Prometheus is configured as a Grafana datasource

## Logging Flow

Validated path:

Kubernetes workload logs
→ Grafana Alloy
→ Loki
→ Grafana

Alloy discovers Kubernetes pod logs using the Kubernetes API.

Loki successfully receives logs from the reliability environment,
including traffic generator summary messages.

Grafana is provisioned with Loki as a datasource.

## Automated Validation

The script:

`observability/scripts/validate-kind-observability.sh`

checks:

1. Kind context
2. workload availability
3. ServiceMonitor presence
4. metrics endpoints
5. Prometheus availability
6. Prometheus scrape results
7. application HTTP metrics
8. Loki ingestion
9. Grafana datasource provisioning
10. Grafana-to-Loki connectivity

Successful execution finishes with:

`EnvForge observability validation PASS`

## Cloud Mapping

The local functional mapping is:

- Azure Monitor Workspace → Prometheus
- Log Analytics → Loki
- Azure Managed Grafana → local Grafana
- AKS workload integration → Kind workload integration

No Azure deployment or Terraform apply is required for this validation.

## Result

The EnvForge Kind environment is connected end-to-end to the local
observability stack for both metrics and logs.
