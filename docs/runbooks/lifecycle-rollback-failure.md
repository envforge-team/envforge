# Runbook - Lifecycle rollback failure

## 1. Find the environment and job

```bash
docker compose exec -T postgres \
  psql -U envforge -d envforge \
  -c "SELECT id,name,namespace,status
      FROM environments
      ORDER BY created_at DESC
      LIMIT 10;"
```

```bash
docker compose exec -T postgres \
  psql -U envforge -d envforge \
  -c "SELECT environment_id,status,attempt_count,target_revision,last_error
      FROM lifecycle_job
      WHERE action IN ('ROLLBACK','RETRY_ROLLBACK')
      ORDER BY created_at DESC
      LIMIT 10;"
```

## 2. Inspect Helm history

```bash
helm --kube-context kind-envforge \
  history <release> \
  -n <namespace>
```

The requested revision must exist.

## 3. Inspect workload health

```bash
kubectl --context kind-envforge \
  get pods \
  -n <namespace>

kubectl --context kind-envforge \
  get deployments \
  -n <namespace>
```

## 4. Verify worker permissions

```bash
kubectl --context kind-envforge-cleanup-worker \
  auth can-i patch deployments --all-namespaces

kubectl --context kind-envforge-cleanup-worker \
  auth can-i update secrets --all-namespaces
```

## Recovery

Correct an invalid/nonexistent revision before retrying.

If the service-account token expired:

```bash
./scripts/kind-configure-cleanup-worker-rbac.sh
```

Restart the worker after refreshing the context.

Do not manually set the environment to `READY`; the worker sets it to `READY`
only after rollback verification succeeds.
