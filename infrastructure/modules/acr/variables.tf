variable "name" {
  description = "Name of the Azure Container Registry."
  type        = string

  validation {
    condition     = length(trimspace(var.name)) > 0
    error_message = "The ACR name must not be empty."
  }
}

variable "resource_group_name" {
  description = "Name of the resource group hosting the ACR."
  type        = string
}

variable "location" {
  description = "Azure region where the ACR is deployed."
  type        = string
}

variable "sku" {
  description = "SKU of the Azure Container Registry."
  type        = string
  default     = "Basic"

  validation {
    condition     = contains(["Basic", "Standard", "Premium"], var.sku)
    error_message = "ACR SKU must be Basic, Standard, or Premium."
  }
}

variable "tags" {
  description = "Tags applied to the Azure Container Registry."
  type        = map(string)
  default     = {}
}

