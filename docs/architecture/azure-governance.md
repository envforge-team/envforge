# Azure governance

## Scope

EnvForge initially uses one shared Azure development environment.

```text
Environment: dev
Region: northeurope
Region abbreviation: neu
```

## Naming convention

```text
<resource>-<project>-<environment>-<region>-<instance>
```

Examples:

| Resource | Name |
| --- | --- |
| Resource group | `rg-envforge-dev-neu-001` |
| Virtual network | `vnet-envforge-dev-neu-001` |
| AKS subnet | `snet-aks-envforge-dev-neu-001` |
| AKS | `aks-envforge-dev-neu-001` |
| ACR | `acrenvforgedevneu001` |

Resources that do not allow hyphens use lowercase alphanumeric names.

## Tags

```hcl
{
  project     = "envforge"
  environment = "dev"
  managed_by  = "terraform"
  repository  = "envforge-team/envforge"
  team        = "envforge-team"
  purpose     = "portfolio"
}
```

## Network addressing

```text
Virtual network:           10.20.0.0/16
AKS subnet:                10.20.0.0/22
Private endpoints subnet:  10.20.4.0/24
Kubernetes service CIDR:   10.30.0.0/16
Kubernetes DNS IP:         10.30.0.10
```

The Kubernetes service CIDR must not overlap with the Azure virtual network.

## Environments

Only the `dev` environment is provisioned during the MVP.

Temporary application environments are Kubernetes namespaces inside the shared AKS cluster, not separate Azure resource groups or clusters.

## Cost controls

- one shared AKS cluster;
- one Azure region;
- one development environment;
- Azure budget alerts;
- fixed Kubernetes resource profiles;
- automatic namespace expiration;
- regular resource cleanup;
- review before every Terraform apply.

Azure budgets send notifications but do not automatically stop spending.

## Terraform responsibilities

| Module | Owner |
| --- | --- |
| Network | M1 |
| ACR | M2 |
| Monitoring | M3 |
| Remote state | M4 |
| AKS and identities | M5 |

## Safety rules

- Do not commit Azure credentials.
- Do not commit Terraform state.
- Do not run `terraform apply` without reviewing the plan.
- Do not delete a shared resource group without team confirmation.
- Use GitHub OIDC instead of long-lived Azure credentials.