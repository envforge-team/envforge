module "aks" {
  source = "../../modules/aks"

  resource_group_name = data.azurerm_resource_group.envforge.name
  location            = var.location

  cluster_name = var.aks_cluster_name
  dns_prefix   = var.aks_dns_prefix

  aks_subnet_id = module.network.aks_subnet_id

  control_plane_identity_id  = module.identities.aks_control_plane_identity_id
  kubelet_identity_id        = module.identities.aks_kubelet_identity_id
  kubelet_identity_client_id = module.identities.aks_kubelet_identity_client_id
  kubelet_identity_object_id = module.identities.aks_kubelet_identity_principal_id

  service_cidr   = var.aks_service_cidr
  dns_service_ip = var.aks_dns_service_ip

  default_node_pool_vm_size    = var.aks_default_node_pool_vm_size
  default_node_pool_node_count = var.aks_default_node_pool_node_count

  tags = var.tags
}
