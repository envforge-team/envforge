variable "resource_group_name" {
  description = "Name of the existing Azure resource group in which the identities are created."
  type        = string
  validation {
    condition     = length(trimspace(var.resource_group_name)) > 0
    error_message = "The resource group name must not be empty."
  }
}

variable "location" {
  description = "Azure region in which the identities are created."
  type        = string
  validation {
    condition     = length(trimspace(var.location)) > 0
    error_message = "The Azure location must not be empty."
  }
}

variable "control_plane_identity_name" {
  description = "Name of the user-assigned managed identity used by the AKS control plane."
  type        = string
  default     = "id-aks-controlplane-envforge-dev"
  validation {
    condition     = length(trimspace(var.control_plane_identity_name)) > 0
    error_message = "The control plane identity name must not be empty."
  }
}

variable "kubelet_identity_name" {
  description = "Name of the user-assigned managed identity used by the AKS kubelet (node image pulls)."
  type        = string
  default     = "id-aks-kubelet-envforge-dev"
  validation {
    condition     = length(trimspace(var.kubelet_identity_name)) > 0
    error_message = "The kubelet identity name must not be empty."
  }
}

variable "tags" {
  description = "Tags applied to resources that support Azure tags."
  type        = map(string)
  default     = {}
}
