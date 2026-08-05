output "virtual_network_id" {
  description = "Resource ID of the Azure Virtual Network."
  value       = azurerm_virtual_network.this.id
}

output "virtual_network_name" {
  description = "Name of the Azure Virtual Network."
  value       = azurerm_virtual_network.this.name
}

output "virtual_network_address_space" {
  description = "Address spaces assigned to the Azure Virtual Network."
  value       = azurerm_virtual_network.this.address_space
}

output "aks_subnet_id" {
  description = "Resource ID of the subnet used by AKS."
  value       = azurerm_subnet.aks.id
}

output "aks_subnet_name" {
  description = "Name of the subnet used by AKS."
  value       = azurerm_subnet.aks.name
}

output "aks_subnet_address_prefixes" {
  description = "Address prefixes assigned to the AKS subnet."
  value       = azurerm_subnet.aks.address_prefixes
}

output "private_endpoints_subnet_id" {
  description = "Resource ID of the subnet reserved for Private Endpoints."
  value       = azurerm_subnet.private_endpoints.id
}

output "private_endpoints_subnet_name" {
  description = "Name of the subnet reserved for Private Endpoints."
  value       = azurerm_subnet.private_endpoints.name
}

output "private_endpoints_subnet_address_prefixes" {
  description = "Address prefixes assigned to the Private Endpoints subnet."
  value       = azurerm_subnet.private_endpoints.address_prefixes
}