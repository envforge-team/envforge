# EnvForge workload chart

Generic Helm chart used to install resources inside a temporary EnvForge namespace.

The namespace is created by the provisioning flow before the chart is installed. It is deleted separately after Helm cleanup is verified.

## Validate

```bash
helm lint deployment/helm/envforge-workload
```

## Render

```bash
helm template \
  static-demo-m1 \
  deployment/helm/envforge-workload \
  --namespace env-static-demo-m1
```

## Install

```bash
helm upgrade --install \
  static-demo-m1 \
  deployment/helm/envforge-workload \
  --namespace env-static-demo-m1 \
  --values \
  deployment/helm/envforge-workload/values-m1-test.yaml
```

## Inspect

```bash
helm list --namespace env-static-demo-m1

kubectl get resourcequota \
  --namespace env-static-demo-m1

kubectl get limitrange \
  --namespace env-static-demo-m1
```

## Uninstall

```bash
helm uninstall \
  static-demo-m1 \
  --namespace env-static-demo-m1
```