# EnvForge Monitoring Retention Strategy

## Scope

This document defines the local observability retention strategy and the
minimum monitoring level required for EnvForge environments running on Kind.

The local observability stack is based on:

- Prometheus for metrics
- Grafana for dashboards and visualization
- Loki for centralized logs when the logging stack is enabled
- ServiceMonitor resources for workload discovery
- Spring Boot Actuator and Micrometer for application metrics

Azure monitoring resources are not provisioned in the current project
environment. Azure infrastructure remains represented as infrastructure as
code where applicable, while the functional Kubernetes environment is Kind.

## Monitoring Levels

### Platform monitoring

The platform should expose enough information to determine whether the
EnvForge control plane and observability stack are operational.

Required signals:

- Kubernetes pod availability
- readiness and liveness status
- Prometheus scrape status
- application JVM metrics
- container and process resource usage
- monitoring component availability

### Environment monitoring

Every monitored EnvForge environment should expose:

- environment health status
- CPU usage
- memory usage
- HTTP request rate
- HTTP error rate
- recent operational events
- readiness state
- liveness state

Workloads that expose Prometheus metrics should be discovered through a
ServiceMonitor or an equivalent Prometheus scrape configuration.

### Application monitoring

Instrumented applications should expose:

- request count
- request duration
- HTTP response status
- JVM memory usage
- process CPU usage
- application startup and readiness information

For Spring Boot applications, the preferred metrics endpoint is:

`/actuator/prometheus`

## Retention

### Prometheus metrics

Local development retention target:

- 7 days

Reasoning:

- sufficient for local debugging and short demonstrations
- allows comparison between multiple test sessions
- limits local disk usage
- historical long-term analytics are outside the scope of the local Kind
  environment

Prometheus retention should therefore be configured with:

`--storage.tsdb.retention.time=7d`

or the equivalent Helm configuration.

### Logs

Target retention when Loki is enabled:

- 3 days for local development

Logs can consume substantially more storage than metrics, so shorter
retention is sufficient for incident reproduction and demonstrations.

The logging solution should not be treated as permanent archival storage.

### Environment events

Environment events stored by Control API are operational application data
rather than raw monitoring telemetry.

Recommended local retention:

- 30 days

The application may later introduce scheduled cleanup based on project
requirements.

### Health snapshots

Health snapshots are lightweight summarized monitoring records.

Recommended retention:

- 7 days

This provides enough history for local monitoring charts without creating
unbounded PostgreSQL growth.

## Scrape Intervals

Default workload metrics interval:

- 15 seconds

High-value local reliability demonstrations may use:

- 5 seconds temporarily

Short intervals should only be used where faster feedback is needed because
they increase Prometheus storage and query load.

## Availability Requirements

Monitoring must not prevent an environment from operating.

If Prometheus, Grafana, or the logging stack becomes unavailable:

- workload traffic must continue
- Control API should remain available
- environment lifecycle operations should not depend on Grafana
- failures in observability components should be visible but non-blocking

## Minimum Monitoring Definition

An environment is considered monitoring-enabled when:

1. the workload is running
2. readiness and liveness endpoints are available
3. Prometheus can scrape the workload
4. the Prometheus target reports `up = 1`
5. CPU and memory metrics are available
6. request rate and HTTP status metrics are available
7. operational events can be exposed through the Monitoring API

## Local Kind Strategy

The functional monitoring environment is validated on Kind.

Expected flow:

Workload
→ Actuator/Micrometer
→ Service
→ ServiceMonitor
→ Prometheus
→ Grafana / Monitoring API

Logs will follow:

Workload logs
→ log collector
→ Loki
→ Grafana

No `terraform apply` is required for the local monitoring environment.
