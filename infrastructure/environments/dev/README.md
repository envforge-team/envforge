# EnvForge Development Environment

This directory is the Terraform root module for the EnvForge development
environment in Azure.

## Existing resources

The Azure resource group already exists and is read through the
`azurerm_resource_group` data source.

Terraform must not attempt to recreate the resource group.

## Network layout

| Component | Value |
| --- | --- |
| Resource group | `rg-reliability-platform-dev` |
| Virtual Network | `vnet-envforge-dev` |
| VNet address space | `10.20.0.0/16` |
| AKS subnet | `10.20.0.0/22` |
| Private Endpoints subnet | `10.20.4.0/24` |
| Planned Kubernetes service CIDR | `10.30.0.0/16` |
| Planned Kubernetes DNS IP | `10.30.0.10` |

The Kubernetes service CIDR does not overlap the Virtual Network address
space.

## Authentication

Authenticate through Azure CLI:

```bash
az login --use-device-code
az account set --subscription "<subscription-id>"
export ARM_SUBSCRIPTION_ID="$(az account show --query id --output tsv)"
```

Subscription IDs, tenant IDs and credentials must not be committed.

## Initialize

Until the remote backend is configured:

```bash
terraform init -backend=false
```

## Format

```bash
terraform fmt -check -recursive
```

## Validate

```bash
terraform validate
```

## Plan

```bash
terraform plan -var-file=dev.tfvars
```

Review the complete plan before running `terraform apply`.

## Safety

Do not run `terraform apply` or `terraform destroy` without team approval.

Never commit:

- Terraform state files
- Saved Terraform plan files
- Azure credentials
- Client secrets
- The `.terraform` directory