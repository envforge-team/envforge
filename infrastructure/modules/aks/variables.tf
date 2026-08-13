variable "resource_group_name" {
  description = "Name of the existing Azure resource group in which the AKS cluster is created."
  type        = string
  validation {
    condition     = length(trimspace(var.resource_group_name)) > 0
    error_message = "The resource group name must not be empty."
  }
}

variable "location" {
  description = "Azure region in which the AKS cluster is created."
  type        = string
  validation {
    condition     = length(trimspace(var.location)) > 0
    error_message = "The Azure location must not be empty."
  }
}

variable "cluster_name" {
  description = "Name of the AKS cluster."
  type        = string
  default     = "aks-envforge-dev"
  validation {
    condition     = length(trimspace(var.cluster_name)) > 0
    error_message = "The cluster name must not be empty."
  }
}

variable "dns_prefix" {
  description = "DNS prefix for the AKS cluster API server."
  type        = string
  default     = "envforge-dev"
  validation {
    condition     = length(trimspace(var.dns_prefix)) > 0
    error_message = "The DNS prefix must not be empty."
  }
}

variable "kubernetes_version" {
  description = "Kubernetes version for the AKS cluster. Null uses the default version offered by Azure."
  type        = string
  default     = null
}

variable "aks_subnet_id" {
  description = "Resource ID of the subnet the AKS nodes are attached to."
  type        = string
  validation {
    condition     = length(trimspace(var.aks_subnet_id)) > 0
    error_message = "The AKS subnet ID must not be empty."
  }
}

variable "control_plane_identity_id" {
  description = "Resource ID of the user-assigned identity used by the AKS control plane."
  type        = string
  validation {
    condition     = length(trimspace(var.control_plane_identity_id)) > 0
    error_message = "The control plane identity ID must not be empty."
  }
}

variable "kubelet_identity_id" {
  description = "Resource ID of the user-assigned identity used by the AKS kubelet."
  type        = string
  validation {
    condition     = length(trimspace(var.kubelet_identity_id)) > 0
    error_message = "The kubelet identity ID must not be empty."
  }
}

variable "kubelet_identity_client_id" {
  description = "Client ID of the user-assigned identity used by the AKS kubelet."
  type        = string
  validation {
    condition     = length(trimspace(var.kubelet_identity_client_id)) > 0
    error_message = "The kubelet identity client ID must not be empty."
  }
}

variable "kubelet_identity_object_id" {
  description = "Principal (object) ID of the user-assigned identity used by the AKS kubelet."
  type        = string
  validation {
    condition     = length(trimspace(var.kubelet_identity_object_id)) > 0
    error_message = "The kubelet identity object ID must not be empty."
  }
}

variable "service_cidr" {
  description = "CIDR used for Kubernetes services. Must not overlap the Virtual Network address space."
  type        = string
  default     = "10.30.0.0/16"
  validation {
    condition     = can(cidrnetmask(var.service_cidr))
    error_message = "The service CIDR must be a valid CIDR block."
  }
}

variable "dns_service_ip" {
  description = "IP address assigned to the Kubernetes DNS service. Must be inside service_cidr."
  type        = string
  default     = "10.30.0.10"
}

variable "default_node_pool_vm_size" {
  description = "VM size used by the default (system) node pool."
  type        = string
  default     = "Standard_B2s"
  validation {
    condition     = length(trimspace(var.default_node_pool_vm_size)) > 0
    error_message = "The default node pool VM size must not be empty."
  }
}

variable "default_node_pool_node_count" {
  description = "Fixed number of nodes in the default (system) node pool."
  type        = number
  default     = 1
  validation {
    condition     = var.default_node_pool_node_count >= 1
    error_message = "The default node pool must have at least one node."
  }
}

variable "tags" {
  description = "Tags applied to resources that support Azure tags."
  type        = map(string)
  default     = {}
}
