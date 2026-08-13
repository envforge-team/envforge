resource "azurerm_user_assigned_identity" "aks_control_plane" {
  name                = var.control_plane_identity_name
  resource_group_name = var.resource_group_name
  location            = var.location
  tags                = var.tags
}

resource "azurerm_user_assigned_identity" "aks_kubelet" {
  name                = var.kubelet_identity_name
  resource_group_name = var.resource_group_name
  location            = var.location
  tags                = var.tags
}
