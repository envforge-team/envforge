# Day 35 - Incident and Alert End-to-End Validation

## Goal

Validate the complete EnvForge reliability monitoring flow from a
controlled workload incident to Prometheus alerting and recovery.

## Validation Flow

The end-to-end scenario validates:

traffic-generator
-> reliability-demo-api
-> controlled HTTP 5xx incident
-> Micrometer HTTP metrics
-> Prometheus
-> reliability recording rules
-> EnvForgeReliabilityHigh5xxRatio alert
-> workload recovery
-> alert resolution

## Controlled Incident

A single running reliability-demo-api pod is selected as the incident
target.

The protected fault-injection endpoint is called using the runtime-only
incident administration key.

The selected pod is configured to return HTTP 500 responses from
`GET /work`.

The remaining replica stays healthy, allowing EnvForge to demonstrate a
partial workload failure rather than a complete outage.

## Metric Validation

The test queries the Prometheus recording rule:

`envforge_reliability:http_5xx_ratio:rate5m`

The scenario waits until the observed 5xx ratio exceeds the alert
threshold of 5 percent.

This confirms that workload failures are successfully captured by the
application metrics and processed by Prometheus.

## Alert Validation

The test verifies the Prometheus alert:

`EnvForgeReliabilityHigh5xxRatio`

The expected lifecycle is:

- inactive before the incident
- pending after the 5xx threshold is exceeded
- firing after the configured alert duration
- resolved after workload recovery

## Recovery

After the alert reaches the firing state, the incident is reset using
the protected administration endpoint.

Validation confirms that:

- the affected pod returns HTTP 200 again
- the reliability deployment remains available
- the traffic-generator remains available
- the 5xx ratio decreases as the five-minute rate window expires
- the alert eventually resolves

## Cleanup

The validation script uses cleanup handling to reset the controlled
incident if the test exits unexpectedly.

The runtime incident key is read from the Kubernetes Secret and is never
printed or stored in the repository.

## Implementation

The E2E validation is implemented in:

`observability/scripts/validate-incident-alert-e2e.sh`

After the incident test, the standard Kind observability validator is
also executed to confirm that the complete monitoring stack remains
healthy.

## Result

The complete reliability monitoring chain was successfully validated:

incident
-> metric
-> recording rule
-> alert
-> recovery
-> alert resolution

This demonstrates that EnvForge can detect a controlled workload
failure, surface it through Prometheus alerting, and recover cleanly.
