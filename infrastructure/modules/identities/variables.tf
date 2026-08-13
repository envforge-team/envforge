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

variable "aks_subnet_id" {
  description = "Resource ID of the AKS subnet, used to scope the control plane identity's Network Contributor role."
  type        = string
  validation {
    condition     = length(trimspace(var.aks_subnet_id)) > 0
    error_message = "The AKS subnet ID must not be empty."
  }
}

variable "resource_group_id" {
  description = "Resource ID of the resource group, used to scope the GitHub Actions identity's Contributor role."
  type        = string
  validation {
    condition     = length(trimspace(var.resource_group_id)) > 0
    error_message = "The resource group ID must not be empty."
  }
}

variable "acr_id" {
  description = "Resource ID of the Azure Container Registry. Null skips the AcrPull role assignment until the ACR module (M2) is available."
  type        = string
  default     = null
}

variable "github_actions_identity_name" {
  description = "Name of the user-assigned managed identity used by GitHub Actions via OIDC federation."
  type        = string
  default     = "id-github-actions-envforge-dev"
  validation {
    condition     = length(trimspace(var.github_actions_identity_name)) > 0
    error_message = "The GitHub Actions identity name must not be empty."
  }
}

variable "github_repository" {
  description = "GitHub repository (org/repo) trusted to federate with the GitHub Actions identity."
  type        = string
  default     = "envforge-team/envforge"
  validation {
    condition     = length(trimspace(var.github_repository)) > 0
    error_message = "The GitHub repository must not be empty."
  }
}

variable "github_federated_ref" {
  description = "Git ref (branch) allowed to exchange an OIDC token for Azure access."
  type        = string
  default     = "refs/heads/main"
  validation {
    condition     = length(trimspace(var.github_federated_ref)) > 0
    error_message = "The GitHub federated ref must not be empty."
  }
}
