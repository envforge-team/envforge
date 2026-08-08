variable "resource_group_name" {
  description = "Name of the existing resource group used by the EnvForge development environment."
  type        = string

  validation {
    condition     = length(trimspace(var.resource_group_name)) > 0
    error_message = "The resource group name must not be empty."
  }
}

variable "virtual_network_name" {
  description = "Name of the Virtual Network used by EnvForge."
  type        = string

  validation {
    condition     = length(trimspace(var.virtual_network_name)) > 0
    error_message = "The Virtual Network name must not be empty."
  }
}

variable "virtual_network_address_space" {
  description = "Address spaces assigned to the EnvForge Virtual Network."
  type        = list(string)
  default     = ["10.20.0.0/16"]
}

variable "aks_subnet_address_prefixes" {
  description = "Address prefixes assigned to the AKS subnet."
  type        = list(string)
  default     = ["10.20.0.0/22"]
}

variable "private_endpoints_subnet_address_prefixes" {
  description = "Address prefixes assigned to the Private Endpoints subnet."
  type        = list(string)
  default     = ["10.20.4.0/24"]
}

variable "tags" {
  description = "Common tags applied to EnvForge Azure resources."
  type        = map(string)
  default     = {}
}


variable "location" {
  description = "Allowed Azure region used for EnvForge development resources."
  type        = string

  validation {
    condition     = length(trimspace(var.location)) > 0
    error_message = "The Azure deployment location must not be empty."
  }
}