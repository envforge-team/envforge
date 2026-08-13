resource_group_name  = "rg-reliability-platform-dev"
virtual_network_name = "vnet-envforge-dev"
location             = "italynorth"

virtual_network_address_space = [
  "10.20.0.0/16"
]

aks_subnet_address_prefixes = [
  "10.20.0.0/22"
]

private_endpoints_subnet_address_prefixes = [
  "10.20.4.0/24"
]

tags = {
  application = "envforge"
  environment = "dev"
  managed-by  = "terraform"
  project     = "reliability-platform"
}
aks_cluster_name                 = "aks-envforge-dev"
aks_dns_prefix                   = "envforge-dev"
aks_control_plane_identity_name  = "id-aks-controlplane-envforge-dev"
aks_kubelet_identity_name        = "id-aks-kubelet-envforge-dev"
aks_service_cidr                 = "10.30.0.0/16"
aks_dns_service_ip               = "10.30.0.10"
aks_default_node_pool_vm_size    = "Standard_B2s"
aks_default_node_pool_node_count = 1

github_actions_identity_name = "id-github-actions-envforge-dev"
github_repository            = "envforge-team/envforge"
github_federated_ref         = "refs/heads/main"
