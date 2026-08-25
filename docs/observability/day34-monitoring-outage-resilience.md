# Day 34 - Monitoring Outage Resilience

## Goal

Verify that Prometheus and Grafana can become unavailable without
blocking the EnvForge workload or synthetic traffic generation.

## Architecture

The EnvForge monitoring path is decoupled from the application workload.

The reliability workload does not depend on Prometheus or Grafana for
request processing.

Monitoring components observe the workload, but they are not required
for the workload to remain available.

## Outage Simulation

The validation script temporarily scales down:

- Prometheus
- Grafana
- Prometheus Operator

The Prometheus Operator is stopped during the simulation so that it does
not immediately reconcile Prometheus back to its desired replica count.

## Platform Validation During Outage

While Prometheus and Grafana are unavailable, validation confirms:

- reliability-demo-api remains available
- GET /work continues returning HTTP 200
- traffic-generator remains available
- traffic-generator continues producing traffic
- the monitoring outage does not block the reliability workload

## Restoration

The script records the original replica counts before the test.

After outage validation, it restores:

- Prometheus
- Grafana
- Prometheus Operator

The script also uses cleanup handling so that monitoring resources are
restored if the test exits unexpectedly.

## Post-Restoration Validation

After restoration, the complete Kind observability validation is run
again.

This confirms that:

- Prometheus targets recover
- recording rules recover
- Loki logs remain queryable
- Grafana datasources and dashboard recover
- monitoring validation returns PASS

## Implementation

The outage test is implemented in:

`observability/scripts/validate-monitoring-outage.sh`

No artificial dependency or fallback logic was added to the Control API.

The current architecture already avoids requiring Prometheus or Grafana
for normal workload operation.

## Result

EnvForge continues serving the reliability workload while the monitoring
stack is unavailable, and the observability stack can be restored
successfully afterward.
