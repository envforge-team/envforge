# Week 3 - Persistence, Jobs and Audit

## Objective

Week 3 adds persistent lifecycle jobs and audit history to cleanup-worker.

The worker still uses the dry-run command runner. No real Helm or Kubernetes command is enabled.

## Deliverables

- Spring Data JPA;
- H2 local database;
- PostgreSQL runtime driver;
- Flyway database migration;
- lifecycle job entity;
- lifecycle audit entity;
- repositories;
- job creation service;
- one-active-job validation;
- audit service;
- persistence tests.

## Important integration boundary

The cleanup-worker does not create a second final `Environment` entity.

The environment model belongs to the component responsible for environment creation. During Week 3, lifecycle jobs store only:

- environment ID;
- namespace name;
- Helm release name;
- actor;
- target revision;
- action;
- job status.

## Verification

```bash
cd /mnt/c/Users/Raul/Desktop/envforge/apps/cleanup-worker
mvn clean test
```

Expected:

```text
BUILD SUCCESS
Failures: 0
Errors: 0
```

Start:

```bash
mvn spring-boot:run
```

Create a delete job:

```bash
curl -i -X POST http://localhost:8080/internal/lifecycle/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "11111111-1111-1111-1111-111111111111",
    "action": "DELETE",
    "actorId": "raul",
    "namespaceName": "env-demo",
    "helmReleaseName": "env-demo-release"
  }'
```

Expected:

```text
201 Created
```

A second active job for the same environment should return:

```text
409 Conflict
```

## Definition of Done

- [ ] Flyway creates both lifecycle tables.
- [ ] JPA validation succeeds.
- [ ] A lifecycle job can be persisted.
- [ ] An audit event is created.
- [ ] A second active job is rejected.
- [ ] Tests pass.
- [ ] No real Helm or Kubernetes command is executed.
