# Lifecycle user stories and acceptance criteria

## US-LC-01 — View expiration

As an environment owner, I want to see when my environment expires so that I know how long it remains available.

Acceptance criteria:

- `expiresAt` is returned by the API.
- Portal shows absolute time and countdown.
- Expired values are visually identifiable.
- UTC is used in storage and API payloads.

## US-LC-02 — Extend lifetime

As an authorized owner, I want to extend lifetime so that an environment is not removed while I still need it.

Acceptance criteria:

- new expiration is in the future;
- deleted/deleting environments are rejected;
- owner, operator or admin authorization is required;
- old and new values are audited;
- a concurrent modification returns a clear conflict.

## US-LC-03 — Automatic expiration

As the platform, I want expired environments to be detected automatically so that temporary resources do not remain indefinitely.

Acceptance criteria:

- scheduler selects only eligible environments;
- the operation is idempotent;
- system identity is used in audit;
- a cleanup job is queued;
- a failure does not stop processing of the remaining environments.

## US-LC-04 — Manual delete

As an authorized user, I want to delete an environment so that unused AKS resources are removed.

Acceptance criteria:

- ownership/role is checked;
- status becomes `DELETING`;
- one asynchronous job is created;
- repeated/conflicting operations are rejected safely;
- cleanup is verified;
- final status is `DELETED`;
- actor and outcome are audited.

## US-LC-05 — Rollback

As an operator, I want to restore a previous Helm revision so that a failed deployment can be recovered.

Acceptance criteria:

- operator/admin authorization;
- allowed source status;
- target revision exists and differs from current revision;
- status becomes `ROLLING_BACK`;
- `helm rollback` uses wait/timeout;
- health is verified;
- status becomes `READY` or `ROLLBACK_FAILED`;
- target revision and actor are audited.

## US-LC-06 — Retry cleanup

As an operator, I want recoverable failures to retry so that temporary Kubernetes errors do not require immediate manual action.

Acceptance criteria:

- bounded attempts;
- delayed retries;
- last error stored;
- every retry audited;
- final status is visible.

## US-LC-07 — Verify cleanup

As the platform, I want to verify resource removal so that the database never claims a sandbox was deleted while resources remain.

Acceptance criteria:

- Helm release is checked;
- namespace/resources are checked according to policy;
- `DELETED` is assigned only after successful verification;
- partial cleanup results in `DELETE_FAILED`.

## US-LC-08 — Audit

As an administrator, I want lifecycle history so that destructive actions are traceable.

Each event contains:

- environment ID;
- actor ID;
- action;
- previous and new status;
- result;
- details/error;
- timestamp.

## US-LC-09 — Prevent concurrent operations

As the platform, I want one active lifecycle job per environment so that delete and rollback cannot corrupt each other.

Acceptance criteria:

- database-level uniqueness for active jobs;
- row locking during state transition;
- HTTP conflict for concurrent requests;
- worker claim uses `FOR UPDATE SKIP LOCKED`.

## US-LC-10 — Least privilege

As a security administrator, I want the worker to have only necessary AKS permissions so that compromise has limited impact.

Acceptance criteria:

- dedicated ServiceAccount;
- no `cluster-admin`;
- operations constrained to EnvForge-owned namespaces where feasible;
- non-root container and restricted security context;
- destructive workflows require protected GitHub environments.
