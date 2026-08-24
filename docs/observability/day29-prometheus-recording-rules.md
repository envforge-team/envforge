# Day 29 - Prometheus Metrics and Recording Rules

## Goal

Finalize Prometheus metrics used by the EnvForge reliability monitoring
workflow and provide precomputed recording rules for dashboards and alerts.

## Recording Rule Group

PrometheusRule:

`envforge-reliability-recording-rules`

Rule group:

`envforge.reliability.recording`

## Recorded Metrics

The following metrics are generated:

- `envforge_reliability:http_requests_per_second:rate5m`
- `envforge_reliability:http_5xx_per_second:rate5m`
- `envforge_reliability:http_5xx_ratio:rate5m`
- `envforge_reliability:http_avg_latency_seconds:rate5m`
- `envforge_reliability:targets_up`
- `envforge_reliability:process_cpu_usage:avg`
- `envforge_reliability:jvm_memory_used_bytes:sum`

These metrics provide precomputed values for:

- request rate
- server error rate
- server error ratio
- average HTTP latency
- monitored target availability
- process CPU usage
- JVM memory usage

## Zero-value Behaviour

The 5xx recording rules return zero when no 5xx time series exists in the
selected interval.

The target availability rule also returns zero when no workload target is
present.

This avoids ambiguous `N/A` values in dashboards and alert expressions.

## Local Validation

The rules were loaded by Prometheus in the local Kind environment.

Validated result:

`envforge_reliability:targets_up = 2`

Request rate was also generated from live traffic produced by the EnvForge
traffic generator.

## CI Validation

The observability validation workflow installs the PrometheusRule resource
and the end-to-end validation script verifies the recorded target count.

This makes the recording rules part of the reproducible Kind observability
validation process.

## Result

Prometheus recording rules are available for the Grafana dashboards and
alert rules implemented in the next observability stage.
