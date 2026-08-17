# Day 24 - Monitoring Validation, Outputs and Costs

## Environment

The functional EnvForge Kubernetes environment is Kind.

Validated Kubernetes context:

`kind-envforge-local`

No Azure monitoring resources are deployed in the functional environment.

## Installed Observability Stack

| Component | Helm chart | Application version |
|---|---|---|
| Grafana Alloy | alloy 1.11.1 | v1.18.1 |
| kube-prometheus-stack | 87.21.0 | v0.92.1 |
| Loki | 18.9.0 | 3.7.6 |

The stack provides:

- Prometheus metrics
- Grafana visualization
- Loki centralized logs
- Grafana Alloy Kubernetes log collection
- ServiceMonitor workload discovery

## Terraform Validation

The monitoring Terraform module was initialized without a backend:

`terraform init -backend=false`

Validation result:

`Success! The configuration is valid.`

Required providers:

- hashicorp/helm >= 3.2.0, < 4.0.0
- hashicorp/kubernetes >= 3.2.0, < 4.0.0

Resolved versions during validation:

- hashicorp/helm 3.2.0
- hashicorp/kubernetes 3.2.1

No `terraform apply` was executed.

### Terraform Plan

A live Terraform plan is intentionally not used for the currently running
Kind monitoring stack.

The functional observability stack was installed through Helm and is not
represented in Terraform state. Running Terraform against the same resources
without importing them would produce a create/adoption plan that does not
represent the actual deployment ownership.

For the local environment, validation is therefore performed with:

- terraform fmt
- terraform init -backend=false
- terraform validate
- helm lint
- helm template
- runtime Kubernetes checks

## Helm Validation

EnvForge monitoring Helm chart:

- charts linted: 1
- charts failed: 0

Rendered successfully:

- kube-prometheus-stack 87.21.0
- Loki 18.9.0
- Alloy 1.11.1

## Kind Capacity

Kind node:

`envforge-local-control-plane`

Capacity:

- CPU: 32 cores
- Memory: approximately 15.57 GiB

## Current Node Resource Allocation

Kubernetes reported:

| Resource | Requests | Limits |
|---|---:|---:|
| CPU | 1210m | 700m |
| Memory | 660 MiB | 966 MiB |

This includes system, monitoring and application workloads running on the
single Kind node.

## Reliability Demo Workload

The monitored reliability workload currently requests:

| Workload | CPU request | Memory request | CPU limit | Memory limit |
|---|---:|---:|---:|---:|
| reliability-demo-api replica 1 | 100m | 128 MiB | 250m | 256 MiB |
| reliability-demo-api replica 2 | 100m | 128 MiB | 250m | 256 MiB |
| traffic-generator | 50m | 64 MiB | 100m | 64 MiB |

Total:

- CPU requests: 250m
- Memory requests: 320 MiB
- CPU limits: 600m
- Memory limits: 576 MiB

## Monitoring Resource Configuration

Alloy currently requests:

- CPU: 10m
- Memory: 50 MiB

The remaining local monitoring components do not currently define explicit
container requests and limits.

This is acceptable for the local Kind development environment.

A production deployment should define explicit requests and limits for all
observability components.

## Metrics Retention

Prometheus retention:

`7d`

This matches the EnvForge local monitoring retention strategy.

Because the current local Prometheus deployment does not use durable
production storage, retention does not guarantee that metrics survive cluster
or pod recreation.

## Log Retention

Loki is configured for approximately:

`72h`

The current Loki deployment uses ephemeral local storage.

Logs may therefore be lost if the Loki pod or Kind cluster is recreated.

This is acceptable for local development and reliability demonstrations but
is not a production storage strategy.

## Cost Assessment

### Cloud

Current functional deployment:

- Azure AKS: not deployed
- Azure Monitor Workspace: not deployed
- Log Analytics Workspace: not deployed
- Azure Managed Grafana: not deployed

Cloud infrastructure cost for the current functional environment:

`N/A`

Azure deployment was not required for the Kind-based local validation.

### Local

The observability stack runs on the developer workstation using:

- Docker Desktop
- Kind
- Kubernetes
- Helm

There is no additional cloud billing associated with this local deployment.

Local CPU, memory, storage and electricity consumption are workstation costs
and are not represented as cloud infrastructure charges.

## Day 24 Result

Validated successfully:

- Terraform module syntax and providers
- Helm chart linting
- Prometheus/Grafana rendering
- Loki rendering
- Alloy rendering
- Kubernetes resource footprint
- Prometheus retention
- local deployment cost model

No Terraform apply was performed.
