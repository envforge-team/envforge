# Static Demo Kubernetes Policies

These manifests create the namespace-level policies used by the M1 provisioning module.

## Resources

- `namespace.yaml`: isolated EnvForge namespace;
- `resource-quota.yaml`: aggregate namespace limits;
- `limit-range.yaml`: default and maximum container resources.

## Validate

```bash
kubectl apply \
  --dry-run=client \
  --filename deployment/kubernetes/examples/static-demo/ \
  --recursive
```

## Install

```bash
kubectl apply \
  --filename deployment/kubernetes/examples/static-demo/namespace.yaml

kubectl apply \
  --filename deployment/kubernetes/examples/static-demo/resource-quota.yaml

kubectl apply \
  --filename deployment/kubernetes/examples/static-demo/limit-range.yaml
```

## Verify

```bash
kubectl get namespace env-static-demo-m1

kubectl describe resourcequota \
  envforge-resource-quota \
  --namespace env-static-demo-m1

kubectl describe limitrange \
  envforge-container-limits \
  --namespace env-static-demo-m1
```

## Delete

```bash
kubectl delete namespace env-static-demo-m1
```

Deleting the namespace also deletes all namespaced resources inside it.