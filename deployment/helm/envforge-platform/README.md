# EnvForge platform chart

Helm chart for the EnvForge platform services (Control API and Portal),
including their RBAC (ServiceAccount/Role/RoleBinding) and NetworkPolicy.

Unlike `envforge-workload` (installed once per ephemeral user
environment), this chart is installed once, into the
`envforge-platform` namespace, and stays up for the lifetime of the
platform itself.

## Validate
```bash
helm lint deployment/helm/envforge-platform
```

## Render
```bash
helm template \
  envforge-platform \
  deployment/helm/envforge-platform \
  --namespace envforge-platform
```

## Install
```bash
kubectl create namespace envforge-platform --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install \
  envforge-platform \
  deployment/helm/envforge-platform \
  --namespace envforge-platform
```

## Inspect
```bash
helm list --namespace envforge-platform
kubectl get pods,svc,networkpolicy -n envforge-platform
kubectl get role,rolebinding -n envforge-platform
```

## Uninstall
```bash
helm uninstall envforge-platform --namespace envforge-platform
```
