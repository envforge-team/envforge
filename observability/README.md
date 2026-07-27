# EnvForge Observability

This directory contains the monitoring and observability configuration used by EnvForge.

## Structure

```text
observability/
├── alerts/
│   └── Prometheus and platform alert rules
├── dashboards/
│   └── Grafana dashboard definitions
├── prometheus/
│   └── Prometheus configuration and recording rules
└── README.md
```

## Planned metrics

EnvForge will initially monitor:

- CPU usage;
- memory usage;
- request rate;
- HTTP 5xx error rate;
- request latency;
- application availability;
- Kubernetes pod restarts;
- environment health status.

## Planned failure scenarios

The monitoring configuration will be tested using controlled scenarios:

- HTTP 5xx responses;
- increased request latency;
- high CPU load;
- pod restart;
- unavailable metrics source;
- unavailable Prometheus or Grafana.

## Planned tools

- Spring Boot Actuator;
- Micrometer;
- Prometheus;
- Grafana;
- Azure Monitor;
- Log Analytics;
- Docker Compose;
- Kubernetes ServiceMonitor.
