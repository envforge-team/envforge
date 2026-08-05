module "network" {
  source = "../../modules/network"

  resource_group_name = data.azurerm_resource_group.envforge.name
  location            = data.azurerm_resource_group.envforge.location

  virtual_network_name = var.virtual_network_name
  address_space        = var.virtual_network_address_space

  aks_subnet_name             = "snet-aks"
  aks_subnet_address_prefixes = var.aks_subnet_address_prefixes

  private_endpoints_subnet_name             = "snet-private-endpoints"
  private_endpoints_subnet_address_prefixes = var.private_endpoints_subnet_address_prefixes

  tags = var.tags
}