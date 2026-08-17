# EnvForge Monitoring Terraform Module

This module describes the local Kubernetes monitoring stack used by EnvForge.

## Target environment

The functional Kubernetes environment is Kind.

The module describes:

- monitoring namespace
- kube-prometheus-stack
- Prometheus retention
- ServiceMonitor discovery
- optional Grafana
- optional Alertmanager
- optional kube-state-metrics
- optional node exporter

## Current project policy

Do not run:

terraform apply

Azure provisioning is currently disabled because the project uses Kind as
the functional Kubernetes environment.

Terraform is used here for infrastructure-as-code design and validation.

Allowed validation commands include:

- terraform fmt
- terraform init -backend=false
- terraform validate

The functional local stack may continue to be installed through Helm while
the Terraform configuration remains a validated representation of the
desired monitoring infrastructure.

## Providers

The parent configuration must provide Kubernetes and Helm providers pointing
to the Kind cluster.

Expected Kubernetes context:

kind-envforge-local

## Retention

The default Prometheus retention is:

7d

This follows the EnvForge local monitoring retention strategy.

## Grafana

Grafana is disabled by default in this module during the initial migration.

It will be enabled as part of the local observability stack together with
centralized logging.
