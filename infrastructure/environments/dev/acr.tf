module "acr" {
  source = "../../modules/acr"

  name                = "acrenvforgedev"
  resource_group_name = data.azurerm_resource_group.envforge.name
  location            = var.location

  sku = "Basic"

  tags = var.tags
}