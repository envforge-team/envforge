# Reliability Demo API manifests

Raw Kubernetes manifests for the Reliability Demo API, used to validate the
deployment/rollout flow before the Helm chart (see `deployment/helm/reliability-demo-api`)
takes over as the source of truth for actual releases.

## Resources
- `namespace.yaml`: creates the managed environment namespace;
- `resource-quota.yaml`: limits total namespace consumption;
- `limit-range.yaml`: defines container defaults and boundaries;
- `deployment.yaml`: the application Deployment, with readiness/liveness probes;
- `service.yaml`: ClusterIP Service exposing port 80 -> 8080.

## Validate

```bash
kubectl apply \
  --dry-run=client \
  -f deployment/kubernetes/reliability-demo-api/
```

## Install

```bash
kubectl apply -f deployment/kubernetes/reliability-demo-api/namespace.yaml
kubectl apply -f deployment/kubernetes/reliability-demo-api/resource-quota.yaml
kubectl apply -f deployment/kubernetes/reliability-demo-api/limit-range.yaml
kubectl apply -f deployment/kubernetes/reliability-demo-api/deployment.yaml
kubectl apply -f deployment/kubernetes/reliability-demo-api/service.yaml
```

## Inspect

```bash
kubectl get pods -n env-reliability-demo -w
kubectl describe deployment reliability-demo-api -n env-reliability-demo
```

## Cleanup

```bash
kubectl delete namespace env-reliability-demo
```
