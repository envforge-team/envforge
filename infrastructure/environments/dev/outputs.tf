output "resource_group_name" {
  description = "Name of the existing resource group used by EnvForge."
  value       = data.azurerm_resource_group.envforge.name
}

output "azure_location" {
  description = "Azure region of the existing resource group."
  value       = data.azurerm_resource_group.envforge.location
}

output "virtual_network_id" {
  description = "Resource ID of the EnvForge Virtual Network."
  value       = module.network.virtual_network_id
}

output "virtual_network_name" {
  description = "Name of the EnvForge Virtual Network."
  value       = module.network.virtual_network_name
}

output "aks_subnet_id" {
  description = "Resource ID of the subnet used by AKS."
  value       = module.network.aks_subnet_id
}

output "private_endpoints_subnet_id" {
  description = "Resource ID of the subnet reserved for Private Endpoints."
  value       = module.network.private_endpoints_subnet_id
}