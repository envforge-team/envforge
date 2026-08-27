# Week 6 - Kind E2E, CI and observability

## Context

cleanup-worker is intentionally not deployed as a pod in Kind for the local
development flow. The existing EnvForge local architecture runs PostgreSQL in
Docker Compose and Control API directly from WSL while temporary environments
run in Kind.

Running cleanup-worker from WSL keeps it on the same database as Control API
and gives it direct access to the existing local kubeconfig.

## Deliverables

- Kind environment validation script;
- helper script that starts cleanup-worker in real Kind mode;
- end-to-end DELETE validation;
- end-to-end expiration validation;
- end-to-end rollback validation;
- GitHub Actions Kind lifecycle workflow;
- Prometheus metrics for lifecycle results and duration;
- Prometheus scraping of cleanup-worker;
- local lifecycle alerts.

## End-to-end flow

```text
Control API
-> shared PostgreSQL
-> cleanup-worker
-> Helm / kubectl
-> kind-envforge
-> audit
-> environment status
```

## Validation

DELETE:

```text
READY -> lifecycle job -> DELETING
-> helm uninstall
-> namespace deletion
-> DELETED
```

Expiration:

```text
expires_at in the past
-> expiration scheduler
-> lifecycle job
-> cleanup
-> DELETED
```

Rollback:

```text
Helm revision 2
-> rollback request targetRevision=1
-> helm rollback
-> latest revision deployed
-> READY
```

## Definition of Done

- Control API and cleanup-worker are healthy;
- PostgreSQL is shared;
- Kind node is Ready;
- DELETE E2E passes;
- expiration E2E passes;
- rollback E2E passes;
- `/actuator/prometheus` exposes lifecycle metrics;
- lifecycle Kind workflow is committed;
- no Azure/AKS resource is required for these tests.
