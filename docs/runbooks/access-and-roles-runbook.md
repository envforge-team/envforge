# Access and roles runbook

Operational steps for managing user roles, checking Kubernetes RBAC,
and troubleshooting authentication/authorization failures in EnvForge.

## Prerequisites

- ADMIN-role account in envforge (for role changes)
- `kubectl` access to the relevant cluster (`kind-envforge` locally)
- `psql` or equivalent access to the Postgres database, for the
  break-glass procedure only

## Granting or changing a user's role

Only ADMIN can do this, via `AuthorizationService.requireAdmin`.

```bash
curl -X PUT "http://localhost:8080/api/users/<user-id>/role" \
  -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"role": "OPERATOR"}'
```

Valid roles: `USER`, `OPERATOR`, `ADMIN`. Any other value returns
`400 Bad Request`. An unknown `<user-id>` returns `404 Not Found`. A
non-ADMIN caller gets `403 Forbidden`.

Every successful role change writes a `SUCCESS` audit event
(`UPDATE_USER_ROLE`); every denied attempt writes a `FAILURE` audit
event, visible to ADMIN via `GET /api/audit`.

## Checking a user's current role

The user themselves: `GET /api/me` with their own token.

Anyone else: there's no per-user lookup endpoint yet; check
`GET /api/audit` for their history, or query the `users` table
directly (read-only) if you have DB access.

## First ADMIN / bootstrap

There's no seed data and no "first user becomes ADMIN" behavior. In
the `entra` profile, `EntraIdCurrentUserProvider` grants `ADMIN`
automatically, on first login only, to whichever identity's email
matches `envforge.security.bootstrap-admin-email`
(`ENVFORGE_BOOTSTRAP_ADMIN_EMAIL`). Every other new identity defaults
to `USER`. Change that property before first deploy to a new
environment, or the bootstrap admin will be whoever happens to match
the default.

## Revoking access

There's no "disable user" or delete endpoint. To remove elevated
access, downgrade the role to `USER` via the role-update endpoint
above — this doesn't revoke a still-valid JWT already issued by Entra
ID (role is re-read from the DB on every request, so the *next*
request after downgrade is correctly restricted; only actions taken
in the current in-flight request, if any, aren't retroactively
undone).

## Break-glass: no ADMIN account is reachable

If the bootstrap-admin email was misconfigured and nobody can reach
`PUT /api/users/{id}/role`, the only path is a direct DB update:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = '<known-good-email>';
```

This bypasses `AuthorizationService` and does **not** write an audit
event — note down who did this, when, and why, outside the system,
since the audit trail won't capture it.

## Kubernetes RBAC (ServiceAccount / Role / RoleBinding)

These are per-workload, not per-user — control-api and portal each run
under their own `ServiceAccount` with a minimal `Role`
(read/list/watch on pods/services/events/deployments/replicasets for
control-api; portal has no `Role` at all, `automountServiceAccountToken: false`).
They are not meant to be edited per-request; changing them changes
what the *workload* can do to the cluster, not what any envforge user
can do.

```bash
kubectl get role,rolebinding -n envforge-platform
kubectl auth can-i list pods \
  --as=system:serviceaccount:envforge-platform:control-api \
  -n envforge-platform
```

## NetworkPolicy troubleshooting

If a pod can't reach another service, check `NetworkPolicy` selectors
before assuming an application bug:

```bash
kubectl get networkpolicy -n envforge-platform
kubectl describe networkpolicy control-api-network-policy -n envforge-platform
```

control-api's ingress is restricted to the portal only; egress is
restricted to postgres (5432), DNS, and HTTPS (443). portal's ingress
is intentionally open (public entrypoint).

## Secret management

control-api's DB password is a Kubernetes `Secret`
(`control-api-db`), not a plain env value:

```bash
kubectl get secret control-api-db -n envforge-platform
kubectl get secret control-api-db -n envforge-platform \
  -o jsonpath='{.data.db-password}' | base64 -d
```

The value itself still originates as plaintext in
`deployment/helm/envforge-platform/values.yaml` — this Secret only
moves *delivery* into Kubernetes, it doesn't yet solve *storage*.
Azure Key Vault migration is a known TODO (see `security-model.md`).

## Troubleshooting authentication/authorization errors

- **401 Unauthorized**: no valid JWT, or the token failed validation
  (bad signature, wrong issuer, expired). Expired tokens are rejected
  by Spring's default `JwtValidators.createDefault()` before any
  application code runs.
- **403 Forbidden**: the JWT is valid, but `CurrentUser.role()`
  doesn't satisfy the endpoint's required role. Check the user's role
  via the DB or ask an ADMIN to check `GET /api/audit` for the
  specific denied action.
- Excessive 401/403 volume triggers the `ExcessiveDeniedAccess`
  Prometheus alert (`observability/alerts/security-access-denied.yml`)
  and is visible on the security-overview Grafana dashboard.
