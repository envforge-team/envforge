# Azure Container Registry (ACR)

## Purpose

EnvForge uses Azure Container Registry (ACR) to store container images used by the AKS cluster.

The ACR is created using the reusable Terraform module located at:

infrastructure/modules/acr

The development environment uses this module from:

infrastructure/environments/dev/acr.tf

## ACR Configuration

The development registry is configured with:

- Name: `acrenvforgedev`
- SKU: `Basic`
- Admin access: disabled

Admin access is disabled because AKS uses its managed kubelet identity to authenticate to the registry.

## Tags

The ACR receives the common EnvForge tags defined in `dev.tfvars`:

tags = {
  application = "envforge"
  environment = "dev"
  managed-by  = "terraform"
  project     = "reliability-platform"
}

## Terraform Module

The ACR Terraform module is located at:

infrastructure/modules/acr/

The module contains:

- main.tf
- variables.tf
- outputs.tf
- versions.tf

The module exposes the following outputs:

- id - Azure resource ID of the ACR
- name - ACR name
- login_server - ACR login server

## AKS Integration

The AKS kubelet identity is granted the Azure AcrPull role on the ACR.

The ACR resource ID is passed to the identities module in:

infrastructure/environments/dev/identities.tf

using:

acr_id = module.acr.id

The identities module creates the role assignment with:

role_definition_name = "AcrPull"

This allows AKS to pull container images from the ACR.

## Validation

The Terraform configuration is formatted and validated using:

terraform fmt
terraform validate

The Terraform plan should be reviewed before applying infrastructure changes.