# Day 35 - Security Review

## Goal

End-of-sprint review of the security posture across authentication,
authorization, secrets, and cluster hardening for EnvForge.

## Scope

- Control API authentication (dev profile, Entra ID profile)
- Authorization / RBAC (`USER` / `OPERATOR` / `ADMIN`)
- Audit logging and security metrics/alerts
- Kubernetes hardening (non-root, NetworkPolicy, RBAC, Secrets)
- Azure / Terraform identity model (linked, not re-covered here)
- Monitoring and reliability access control (owned by teammate, linked, not duplicated)

## Authentication

Two profiles, additive via Spring `@Profile`, neither breaking the other:

- `default` (`!entra`): `DevCurrentUserProvider`, permissive local identity
  used for dev and the existing test suite.
- `entra`: `EntraSecurityConfig` + Spring OAuth2 Resource Server, JWT
  validated against the Microsoft Entra ID issuer. `EntraIdCurrentUserProvider`
  maps JWT claims to a `CurrentUser`, using the DB-persisted role as the
  source of truth and falling back to a configured bootstrap-admin email
  or `USER` for unknown identities.

Negative paths verified (Day 34, `EntraNegativePathsTest`):

- expired token → rejected by the same `JwtValidators.createDefault()`
  validation Spring Boot wires up from `issuer-uri`
- JWT missing identity claims (`preferred_username`/`email`/`name`) →
  falls back to `sub` instead of failing
- valid token, insufficient role → still denied by `AuthorizationService`

## Authorization

`AuthorizationService.requireRole` / `requireAdmin` / `requireOwnerOrAdmin`
enforce purely on `CurrentUser.role()`, which is DB-authoritative and
independent of which `CurrentUserProvider` produced it. Verified for:

- control-api's own endpoints (Day 15, and again through the Entra path
  on Day 34)
- monitoring endpoints, restricted to `OPERATOR`/`ADMIN`
  (see `docs/observability/day32-monitoring-access-control.md`)
- reliability incident endpoints
  (see `docs/observability/day33-incident-and-traffic-generator-security.md`)

## Audit & Metrics

- `AuditService` records authorization denials and role changes.
- `SecurityMetrics` exposes login/401/403 counters (Day 29).
- Grafana dashboard and a Prometheus alert on excessive denied access
  (Day 30).

## Kubernetes Hardening (Day 33)

- Pod- and container-level `securityContext` on control-api and portal:
  `runAsNonRoot`, `seccompProfile: RuntimeDefault`,
  `allowPrivilegeEscalation: false`, all capabilities dropped.
- control-api additionally sets `runAsUser: 100` explicitly, because its
  image's `USER envforge` is a name rather than a numeric UID, and
  kubelet cannot verify `runAsNonRoot` against a non-numeric user without
  it.
- portal switched to `nginxinc/nginx-unprivileged:alpine`, listening on
  8080 instead of 80.
- control-api's DB password is sourced from a Kubernetes `Secret`
  (`control-api-db`) via `secretKeyRef` instead of a plain env value.
- ServiceAccount/Role/RoleBinding scoped to read/list/watch only
  (Day 17, re-reviewed Day 33, no reduction needed).
- NetworkPolicy: control-api ingress restricted to the portal, egress to
  postgres/DNS/HTTPS; portal ingress is intentionally open as the public
  entrypoint (reviewed Day 33, no change needed).
- Verified live on a local Kind cluster: both pods start non-root
  without `CreateContainerConfigError`, Secret is a distinct object from
  the app's ConfigMaps.

## Azure / Terraform

- Managed identities, federated credentials, role assignments (Days
  22-23), scopes reviewed and confirmed minimal (Day 24).
- AKS creation and further `terraform apply` runs are halted team-wide
  pending Azure subscription quota; a local Kind cluster is used instead
  for all Kubernetes work this sprint.
- Full detail in `docs/architecture/azure-identity-model.md`.

## Test Coverage (this pass)

- control-api: 60/60 (`mvn test`)
- cleanup-worker: 12/12
- reliability-demo-api: no test sources yet
- Added this sprint: `EntraNegativePathsTest` (Day 34),
  `EntraIdCurrentUserProviderTest` (Day 32), security metrics/audit/
  authorization/user-controller tests (Day 29), plus teammate's
  monitoring and reliability authorization tests (Days 32-33).

## Known Gaps / Deferred

- Real Entra token happy-path test (`az login` device-code flow) not
  completed, blocked on interactive consent (Day 31).
- AKS deployment blocked by the team-wide `terraform apply` moratorium
  (Day 25).
- An untracked Azure identity (`id-github-actions-envforge-dev`) has not
  been imported into Terraform state.
- `module.network` state/reality drift with M1's changes, unresolved.
- control-api's DB password is now delivered via a Kubernetes Secret,
  but the value itself still originates as plaintext in `values.yaml`;
  Azure Key Vault migration remains a TODO.

## Result

Authentication (dev + Entra), DB-backed RBAC enforcement, audit and
metrics/alerting, and baseline Kubernetes hardening (non-root, dropped
capabilities, NetworkPolicy, RBAC, Secrets) are in place and tested.
Remaining gaps are scoped, documented, and not blocking for this sprint.
