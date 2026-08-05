# Week 4 - Scheduling, Retry and Job Recovery

## Objective

Week 4 adds automatic lifecycle job processing, bounded retry and stale-job recovery.

The implementation continues to use `DryRunLifecycleCommandRunner`.

## Deliverables

- lifecycle job processor;
- delete dry-run flow;
- rollback dry-run flow;
- retry scheduling;
- maximum retry enforcement;
- scheduler for queued jobs;
- stale running-job recovery;
- timeout configuration;
- batch processing;
- tests for success and retry behavior.

## Processing flows

```text
QUEUED -> RUNNING -> SUCCEEDED
```

```text
QUEUED -> RUNNING -> RETRYING -> RUNNING -> SUCCEEDED or FAILED
```

```text
RUNNING past timeout -> RETRYING or FAILED
```

## Verification

```bash
cd /mnt/c/Users/Raul/Desktop/envforge/apps/cleanup-worker
mvn clean test
mvn spring-boot:run
```

Create a job:

```bash
curl -s -X POST http://localhost:8080/internal/lifecycle/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "environmentId": "22222222-2222-2222-2222-222222222222",
    "action": "DELETE",
    "actorId": "raul",
    "namespaceName": "env-week4",
    "helmReleaseName": "env-week4-release"
  }'
echo
```

The scheduler processes it safely through the dry-run runner.

## Safety check

```bash
grep -RniE "ProcessBuilder|Runtime\\.getRuntime|\\.exec\\(" src/main/java \
  || echo "OK: no real command execution found"
```

## Definition of Done

- [ ] Queued jobs are claimed.
- [ ] Jobs become `RUNNING`.
- [ ] Successful jobs become `SUCCEEDED`.
- [ ] Temporary failures become `RETRYING`.
- [ ] Retry count is bounded.
- [ ] Final failures become `FAILED`.
- [ ] Stale jobs are recovered.
- [ ] Audit events are created.
- [ ] Tests pass.
- [ ] No real destructive command is enabled.
