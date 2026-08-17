resource "kubernetes_namespace_v1" "monitoring" {
  metadata {
    name = var.namespace

    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "envforge.io/component"        = "monitoring"
    }
  }
}

resource "helm_release" "kube_prometheus_stack" {
  name       = var.release_name
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = var.kube_prometheus_stack_chart_version
  namespace  = kubernetes_namespace_v1.monitoring.metadata[0].name

  wait    = true
  timeout = 600

  values = [
    yamlencode({
      defaultRules = {
        create = false
      }

      alertmanager = {
        enabled = var.alertmanager_enabled
      }

      grafana = {
        enabled = var.grafana_enabled
      }

      kubeStateMetrics = {
        enabled = var.kube_state_metrics_enabled
      }

      nodeExporter = {
        enabled = var.node_exporter_enabled
      }

      prometheus = {
        prometheusSpec = {
          retention = var.prometheus_retention

          serviceMonitorSelectorNilUsesHelmValues = false
          serviceMonitorSelector                  = {}
          serviceMonitorNamespaceSelector         = {}
        }
      }
    })
  ]
}
