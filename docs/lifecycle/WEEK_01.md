# Week 1 — Setup and design

## Goal

Finish local setup, GitHub organization, user stories, state machine and initial module structure.

## Step 1 — Prepare Git

```bash
git clone REPOSITORY_URL
cd REPOSITORY
git checkout main
git pull
git checkout -b feature/lifecycle-week1
```

Use the actual shared base branch.

## Step 2 — Check tools

Windows:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\check-tools.ps1
```

Linux/macOS:

```bash
./scripts/check-tools.sh
```

Record versions and install missing tools.

## Step 3 — Agree on monorepo structure

Proposed structure:

```text
apps/control-api
apps/cleanup-worker
apps/portal
libs/lifecycle-core
deploy/helm
infrastructure/terraform
observability
runbooks
```

Do not force this structure if the team already approved another one.

## Step 4 — Review user stories

Open:

```text
docs/USER_STORIES.md
```

Create GitHub issues for expiration, extension, delete, rollback, audit, concurrency, retry and verification.

## Step 5 — Review state machine

Open:

```text
docs/STATE_MACHINE.md
```

Hold a short team review. Confirm:

- final status names;
- meaning of `EXPIRED`;
- rollback source states;
- namespace deletion policy;
- ownership of each transition.

## Step 6 — Create the worker structure

Copy:

```text
apps/cleanup-worker
```

At this point it may contain only `README`, `pom.xml` and source directories if the team wants strict weekly commits.

## Step 7 — Coordinate dependencies

Ask Member 2 for:

- revision model;
- release naming;
- rollout verification.

Ask Member 5 for:

- actor identity;
- roles;
- audit requirements;
- RBAC model.

## Step 8 — Open PR

```bash
git add .
git commit -m "Add lifecycle week 1 design"
git push -u origin feature/lifecycle-week1
```

PR checklist:

- tools verified;
- user stories reviewed;
- state machine reviewed;
- module folder added;
- dependencies documented.

## Definition of done

Week 1 is complete when the team accepts the design. Code generation alone does not replace this approval.
