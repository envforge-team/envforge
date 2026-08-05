# EnvForge Azure Network Module

This Terraform module creates the network foundation used by EnvForge.

## Resources

- One Azure Virtual Network
- One subnet for Azure Kubernetes Service
- One subnet reserved for Azure Private Endpoints

The module does not create a resource group. The resource group must already
exist and must be supplied by the calling root module.

## Default network layout

| Component | CIDR |
| --- | --- |
| Virtual Network | `10.20.0.0/16` |
| AKS subnet | `10.20.0.0/22` |
| Private Endpoints subnet | `10.20.4.0/24` |

The Kubernetes service CIDR must not overlap the Virtual Network address space.
The planned service CIDR is `10.30.0.0/16`.

## Usage

```hcl
module "network" {
  source = "../../modules/network"

  resource_group_name = "rg-reliability-platform-dev"
  location            = "westeurope"
  virtual_network_name = "vnet-envforge-dev"

  address_space                            = ["10.20.0.0/16"]
  aks_subnet_address_prefixes              = ["10.20.0.0/22"]
  private_endpoints_subnet_address_prefixes = ["10.20.4.0/24"]

  tags = {
    application = "envforge"
    environment = "dev"
    managed-by  = "terraform"
  }
}
```

The final Azure location must match the location selected by the team for the
development environment.