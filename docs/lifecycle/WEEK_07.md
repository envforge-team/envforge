# Week 7 - Security and Reliability

## Scope

Week 7 hardens the lifecycle flow for the local kind environment.

The target flow is:

```text
authenticated/debug user
-> Control API lifecycle endpoint
-> ownership/role authorization
-> cleanup-worker internal endpoint
-> lifecycle job
-> Helm/Kubernetes operation with limited credentials
-> audit
```

## Day 31 - Actor propagation

Manual delete and rollback requests are issued through the Control API.
The actor is derived from `CurrentUserProvider`; clients do not send `actorId`.

Local development headers:

```text
X-Debug-User-Id
X-Debug-User-Email
X-Debug-User-Name
X-Debug-User-Role
```

Automatic expiration uses actor `SYSTEM`.

## Day 32 - Ownership and authorization

`AuthorizationService.requireOwnerOrAdmin` is used for DELETE and ROLLBACK.

Rules:

- ADMIN may operate on any environment.
- OPERATOR may operate only on an environment they own.
- USER cannot delete or roll back an environment.
- authorization failures are audited.

The environment stores the current user's stable id in `created_by`.
Authorization accepts either the stable id or email for compatibility with older local data.

## Day 33 - Limited Kubernetes credentials

The worker uses:

```text
ServiceAccount: envforge-cleanup-worker
Context:        kind-envforge-cleanup-worker
```

Apply and refresh credentials with:

```bash
./scripts/kind-configure-cleanup-worker-rbac.sh
```

The worker can manage the resources used by the EnvForge workload chart and delete
managed namespaces, but it cannot create ClusterRoles or delete Kubernetes nodes.

## Day 34 - Reliability

The real command runner enforces:

- DNS-label validation for release/namespace names;
- explicit kube context;
- command timeouts;
- managed namespace verification;
- idempotent Helm uninstall;
- Helm release verification after uninstall;
- namespace deletion;
- post-delete namespace verification;
- retry limits and delayed retries;
- stale RUNNING job recovery.

An environment becomes `DELETED` only after the release cleanup and namespace
deletion checks both succeed.

## Day 35 - End-to-end verification

Run, with Control API and cleanup-worker already started:

```bash
./scripts/lifecycle-security-e2e.sh
./scripts/lifecycle-kind-e2e.sh
./scripts/lifecycle-rollback-kind-e2e.sh
```

Expected results:

```text
non-owner OPERATOR DELETE -> HTTP 403
owner OPERATOR DELETE     -> SUCCEEDED
ADMIN override DELETE     -> SUCCEEDED
automatic EXPIRE actor    -> SYSTEM
DELETE actor              -> current user email
ROLLBACK actor            -> current user email
namespace after DELETE    -> absent
Helm release after DELETE -> absent
environment after rollback -> READY
```
