# Runbook - Expiration stuck

## Symptoms

An environment has `expires_at` in the past but is not eventually deleted.

## 1. Find expired environments

```bash
docker compose exec -T postgres \
  psql -U envforge -d envforge \
  -c "SELECT id,name,namespace,status,expires_at
      FROM environments
      WHERE expires_at < NOW()
      ORDER BY expires_at;"
```

## 2. Check expiration jobs

```bash
docker compose exec -T postgres \
  psql -U envforge -d envforge \
  -c "SELECT id,environment_id,action,status,attempt_count,next_retry_at,last_error
      FROM lifecycle_job
      WHERE action='EXPIRE'
      ORDER BY created_at DESC;"
```

## 3. Verify scheduler output

The worker should log a scheduler tick periodically:

```text
Lifecycle scheduler tick
Lifecycle scheduler found ... ready job(s)
```

`@EnableScheduling` must be present in the cleanup-worker configuration.

## 4. Verify SYSTEM actor

```bash
docker compose exec -T postgres \
  psql -U envforge -d envforge \
  -c "SELECT actor_id,action,result,details
      FROM lifecycle_audit
      WHERE action='EXPIRE'
      ORDER BY created_at DESC
      LIMIT 10;"
```

Expiration jobs should use:

```text
actor_id = SYSTEM
```

## Recovery

If the worker is healthy but no scheduler ticks appear, restart it and verify the
scheduler configuration.

If the Kubernetes worker token expired, rerun:

```bash
./scripts/kind-configure-cleanup-worker-rbac.sh
```

Do not directly set the environment to `DELETED`.
