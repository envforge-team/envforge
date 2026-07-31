# M1 provisioning manifests

These manifests demonstrate the namespace-level isolation used by EnvForge.

## Resources

- `namespace.yaml`: creates a managed environment namespace;
- `resource-quota.yaml`: limits total namespace consumption;
- `limit-range.yaml`: defines container defaults and boundaries;
- `test-pod.yaml`: verifies default resource injection;
- `invalid-pod.yaml`: verifies rejection of excessive resources.

## Validate

```bash
kubectl apply \
  --dry-run=client \
  -f deployment/kubernetes/m1-provisioning/
```

## Install

```bash
kubectl apply \
  -f deployment/kubernetes/m1-provisioning/namespace.yaml

kubectl apply \
  -f deployment/kubernetes/m1-provisioning/resource-quota.yaml

kubectl apply \
  -f deployment/kubernetes/m1-provisioning/limit-range.yaml
```

## Inspect

```bash
kubectl describe resourcequota \
  envforge-environment-quota \
  --namespace env-static-demo-m1

kubectl describe limitrange \
  envforge-container-limits \
  --namespace env-static-demo-m1
```

## Cleanup

```bash
kubectl delete namespace env-static-demo-m1
```