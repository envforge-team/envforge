output "aks_control_plane_identity_id" {
  description = "Resource ID of the AKS control plane user-assigned identity."
  value       = azurerm_user_assigned_identity.aks_control_plane.id
}

output "aks_control_plane_identity_principal_id" {
  description = "Principal (object) ID of the AKS control plane user-assigned identity."
  value       = azurerm_user_assigned_identity.aks_control_plane.principal_id
}

output "aks_control_plane_identity_client_id" {
  description = "Client ID of the AKS control plane user-assigned identity."
  value       = azurerm_user_assigned_identity.aks_control_plane.client_id
}

output "aks_kubelet_identity_id" {
  description = "Resource ID of the AKS kubelet user-assigned identity."
  value       = azurerm_user_assigned_identity.aks_kubelet.id
}

output "aks_kubelet_identity_principal_id" {
  description = "Principal (object) ID of the AKS kubelet user-assigned identity."
  value       = azurerm_user_assigned_identity.aks_kubelet.principal_id
}

output "aks_kubelet_identity_client_id" {
  description = "Client ID of the AKS kubelet user-assigned identity."
  value       = azurerm_user_assigned_identity.aks_kubelet.client_id
}
