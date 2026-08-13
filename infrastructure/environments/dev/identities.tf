module "identities" {
  source = "../../modules/identities"

  resource_group_name = data.azurerm_resource_group.envforge.name
  location            = var.location

  control_plane_identity_name = var.aks_control_plane_identity_name
  kubelet_identity_name       = var.aks_kubelet_identity_name

  tags = var.tags
}
