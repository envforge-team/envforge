# Day 26 - Monitoring OIDC Status

## Original Goal

Validate GitHub OIDC access to monitoring resources.

## Current Project Environment

The functional Kubernetes environment is Kind.

The local observability stack uses:

- Prometheus
- Grafana
- Loki
- Grafana Alloy

No Azure Monitor Workspace, Log Analytics Workspace, or Azure Managed Grafana
instance is currently deployed.

## Existing Azure Identity Foundation

The infrastructure code includes a GitHub Actions user-assigned managed
identity and a federated identity credential for GitHub OIDC.

The federated identity uses:

- issuer: `https://token.actions.githubusercontent.com`
- audience: `api://AzureADTokenExchange`
- repository and branch based subject

This provides the infrastructure-as-code foundation for GitHub-to-Azure OIDC.

## Validation Status

Live OIDC access to Azure monitoring resources cannot be validated in the
current functional environment because those Azure monitoring resources are
not deployed.

Running Terraform apply or provisioning additional Azure resources is outside
the current local Kind workflow.

## Local Kind Equivalent

The local observability stack does not require Azure OIDC.

Access to Prometheus, Loki and Grafana is performed inside the local
Kubernetes cluster and through local port-forwarding during validation.

## Day 26 Result

Status:

`N/A - superseded by Kind migration`

The Azure OIDC foundation exists in infrastructure as code, but live
monitoring-resource access is intentionally not validated in the current
Kind-based environment.

No Terraform apply was performed.
