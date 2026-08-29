# Runbook - Lifecycle delete failure

## Symptoms

Typical symptoms:

- environment remains `DELETING`;
- lifecycle job is `RETRYING` or `FAILED`;
- namespace still exists;
- Helm release still exists.

## 1. Inspect the latest lifecycle job

```bash
docker compose exec -T postgres \
  psql -U envforge -d envforge \
  -c "SELECT id,environment_id,action,status,attempt_count,next_retry_at,last_error
      FROM lifecycle_job
      ORDER BY created_at DESC
      LIMIT 10;"
```

## 2. Inspect lifecycle audit

```bash
docker compose exec -T postgres \
  psql -U envforge -d envforge \
  -c "SELECT environment_id,actor_id,action,result,details,created_at
      FROM lifecycle_audit
      ORDER BY created_at DESC
      LIMIT 20;"
```

## 3. Check namespace ownership marker

```bash
kubectl --context kind-envforge \
  get namespaces -L envforge.io/managed
```

The cleanup worker intentionally refuses to delete a namespace that exists but
does not have:

```text
envforge.io/managed=true
```

Do not remove this safety check to make a test pass.

## 4. Check Helm

```bash
helm --kube-context kind-envforge list -A
```

For a specific release:

```bash
helm --kube-context kind-envforge \
  status <release> \
  -n <namespace>
```

## 5. Check worker RBAC

```bash
kubectl --context kind-envforge-cleanup-worker \
  auth can-i delete namespaces

kubectl --context kind-envforge-cleanup-worker \
  auth can-i list secrets --all-namespaces
```

Both should return `yes`.

## 6. Check worker logs

Look for:

```text
Lifecycle scheduler found ... ready job(s)
Processing lifecycle job
Refusing lifecycle cleanup for unmanaged namespace
Command timed out
Helm release still exists
```

## Recovery

Fix the root cause first. The worker performs bounded retries automatically.
Do not manually change an environment to `DELETED`.

If the short-lived Kubernetes token expired, rerun:

```bash
./scripts/kind-configure-cleanup-worker-rbac.sh
```

and restart cleanup-worker.
