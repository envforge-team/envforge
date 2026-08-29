# Week 8 - Finalization

## Day 36 - Bug fixing

No new lifecycle architecture is introduced.

Check specifically:

- scheduler not running;
- jobs stuck in QUEUED/RUNNING;
- duplicate active jobs;
- retries not stopping;
- command timeout handling;
- namespace missing `envforge.io/managed=true`;
- namespace remaining after delete;
- Helm release remaining after delete;
- invalid rollback revision;
- environment status not reaching READY/DELETED;
- wrong lifecycle actor;
- ownership bypass;
- worker running with the administrator Kubernetes context.

## Day 37 - Tests

Run:

```bash
./scripts/week8-final-validation.sh
```

This validates:

- Bash script syntax;
- Helm chart lint;
- Control API tests;
- cleanup-worker tests;
- Git whitespace errors.

The end-to-end tests are run separately because they require the three-terminal
local environment.

## Day 38 - Runbooks

Runbooks:

```text
docs/runbooks/lifecycle-delete-failure.md
docs/runbooks/lifecycle-rollback-failure.md
docs/runbooks/lifecycle-expiration-stuck.md
```

Each runbook starts with database, Helm and Kubernetes observations before any
manual recovery action.

## Day 39 - Demo

With the three-terminal environment running:

```bash
./scripts/lifecycle-demo.sh
```

The demo shows:

```text
READY
-> DELETE request through Control API
-> lifecycle job
-> actor in audit
-> Helm uninstall
-> namespace deletion
-> DELETED
```

Rollback is demonstrated separately with:

```bash
./scripts/lifecycle-rollback-kind-e2e.sh
```

## Day 40 - Release preparation

Before committing:

```bash
git status --short
git diff --check
./scripts/week8-final-validation.sh
```

Do not use `git add .` until the changed-file list has been reviewed.

Suggested commit:

```text
feat: harden lifecycle security and reliability
```

The team should create the final release/tag only after all modules have passed
their final integration checks.
