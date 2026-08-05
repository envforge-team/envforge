variable "resource_group_name" {
  description = "Name of the existing Azure resource group in which the network resources are created."
  type        = string

  validation {
    condition     = length(trimspace(var.resource_group_name)) > 0
    error_message = "The resource group name must not be empty."
  }
}

variable "location" {
  description = "Azure region in which the network resources are created."
  type        = string

  validation {
    condition     = length(trimspace(var.location)) > 0
    error_message = "The Azure location must not be empty."
  }
}

variable "virtual_network_name" {
  description = "Name of the Azure Virtual Network."
  type        = string

  validation {
    condition     = length(trimspace(var.virtual_network_name)) > 0
    error_message = "The virtual network name must not be empty."
  }
}

variable "address_space" {
  description = "Address spaces assigned to the Azure Virtual Network."
  type        = list(string)
  default     = ["10.20.0.0/16"]

  validation {
    condition = (
      length(var.address_space) > 0 &&
      alltrue([
        for cidr in var.address_space :
        can(cidrnetmask(cidr))
      ])
    )

    error_message = "At least one valid CIDR address space must be provided."
  }
}

variable "aks_subnet_name" {
  description = "Name of the subnet used by Azure Kubernetes Service."
  type        = string
  default     = "snet-aks"

  validation {
    condition     = length(trimspace(var.aks_subnet_name)) > 0
    error_message = "The AKS subnet name must not be empty."
  }
}

variable "aks_subnet_address_prefixes" {
  description = "CIDR address prefixes assigned to the AKS subnet."
  type        = list(string)
  default     = ["10.20.0.0/22"]

  validation {
    condition = (
      length(var.aks_subnet_address_prefixes) > 0 &&
      alltrue([
        for cidr in var.aks_subnet_address_prefixes :
        can(cidrnetmask(cidr))
      ])
    )

    error_message = "At least one valid CIDR prefix must be provided for the AKS subnet."
  }
}

variable "private_endpoints_subnet_name" {
  description = "Name of the subnet reserved for Azure Private Endpoints."
  type        = string
  default     = "snet-private-endpoints"

  validation {
    condition     = length(trimspace(var.private_endpoints_subnet_name)) > 0
    error_message = "The Private Endpoints subnet name must not be empty."
  }
}

variable "private_endpoints_subnet_address_prefixes" {
  description = "CIDR address prefixes assigned to the Private Endpoints subnet."
  type        = list(string)
  default     = ["10.20.4.0/24"]

  validation {
    condition = (
      length(var.private_endpoints_subnet_address_prefixes) > 0 &&
      alltrue([
        for cidr in var.private_endpoints_subnet_address_prefixes :
        can(cidrnetmask(cidr))
      ])
    )

    error_message = "At least one valid CIDR prefix must be provided for the Private Endpoints subnet."
  }
}

variable "tags" {
  description = "Tags applied to resources that support Azure tags."
  type        = map(string)
  default     = {}
}