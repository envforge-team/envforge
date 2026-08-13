module "identities" {
  source = "../../modules/identities"

  resource_group_name = data.azurerm_resource_group.envforge.name
  location            = var.location

  control_plane_identity_name = var.aks_control_plane_identity_name
  kubelet_identity_name       = var.aks_kubelet_identity_name

  aks_subnet_id     = module.network.aks_subnet_id
  resource_group_id = data.azurerm_resource_group.envforge.id
  acr_id            = var.acr_id

  github_actions_identity_name = var.github_actions_identity_name
  github_repository            = var.github_repository
  github_federated_ref         = var.github_federated_ref

  tags = var.tags
}
