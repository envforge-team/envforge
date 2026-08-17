variable "namespace" {
  description = "Kubernetes namespace used by the local observability stack."
  type        = string
  default     = "monitoring"
}

variable "release_name" {
  description = "Helm release name for kube-prometheus-stack."
  type        = string
  default     = "kube-prometheus-stack"
}

variable "kube_prometheus_stack_chart_version" {
  description = "Pinned kube-prometheus-stack Helm chart version."
  type        = string
  default     = "87.21.0"
}

variable "prometheus_retention" {
  description = "Prometheus local metrics retention period."
  type        = string
  default     = "7d"
}

variable "grafana_enabled" {
  description = "Whether Grafana bundled with kube-prometheus-stack is enabled."
  type        = bool
  default     = false
}

variable "alertmanager_enabled" {
  description = "Whether Alertmanager is enabled in the local stack."
  type        = bool
  default     = false
}

variable "kube_state_metrics_enabled" {
  description = "Whether kube-state-metrics is enabled."
  type        = bool
  default     = false
}

variable "node_exporter_enabled" {
  description = "Whether Prometheus node exporter is enabled."
  type        = bool
  default     = false
}
