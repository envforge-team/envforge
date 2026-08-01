# M1 Kubernetes provisioning failure tests

This runbook validates failure handling for the local M1 provisioning flow.

## Safety requirement

All tests must run against:

```text
docker-desktop
```

Verify:

```bash
kubectl config current-context
```

## Tested scenarios

### Invalid Helm values

Expected result: rejected by `values.schema.json`.

### Missing namespace

Expected result: Helm installation fails because EnvForge must create and label the namespace first.

### Repeated provisioning

Expected result: one Helm release and one set of Kubernetes resources.

### Excessive container resources

Expected result: rejected by `LimitRange`.

### Insecure pod

Expected result: rejected by the restricted Pod Security policy.

### Invalid image

Expected result: rollout failure and automatic Helm rollback when `--atomic` is used.

### Cleanup

Expected flow:

```text
helm uninstall
→ verify resources
→ delete namespace
→ verify deletion
```

## Provision

```bash
./scripts/install-m1-environment-local.sh
```

## Delete

```bash
./scripts/delete-m1-environment-local.sh
```