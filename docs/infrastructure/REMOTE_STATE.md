# Terraform Remote State

EnvForge uses the Terraform `azurerm` backend with Azure Blob Storage.

The committed Terraform configuration contains only:

```hcl
terraform {
  backend "azurerm" {}
}
```

Environment-specific values are passed with:

```bash
terraform init -reconfigure -backend-config=backend.hcl
```

Do not commit `backend.hcl`, `.terraform/`, or local `*.tfstate` files.
