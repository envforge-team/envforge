output "namespace" {
  description = "Namespace used by the monitoring stack."
  value       = kubernetes_namespace_v1.monitoring.metadata[0].name
}

output "prometheus_release_name" {
  description = "Helm release name for kube-prometheus-stack."
  value       = helm_release.kube_prometheus_stack.name
}

output "prometheus_retention" {
  description = "Configured Prometheus retention."
  value       = var.prometheus_retention
}

output "grafana_enabled" {
  description = "Whether Grafana is enabled."
  value       = var.grafana_enabled
}
