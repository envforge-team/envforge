# Day 32 - Monitoring Access Control

## Goal

Define and enforce which EnvForge roles can view operational monitoring
data exposed through the Control API.

## Protected Resources

The access policy applies to:

- `GET /api/environments/{environmentId}/monitoring/metrics`
- `GET /api/environments/{environmentId}/monitoring/events`

These endpoints expose operational metrics and environment events used
for monitoring and troubleshooting.

## Access Policy

| Role | Metrics | Events |
| --- | --- | --- |
| USER | Denied | Denied |
| OPERATOR | Allowed | Allowed |
| ADMIN | Allowed | Allowed |

`USER` receives HTTP `403 Forbidden`.

`OPERATOR` and `ADMIN` are allowed to access monitoring information.

## Implementation

`MonitoringController` uses the existing platform security components:

- `CurrentUserProvider`
- `AuthorizationService`
- `Role`

The controller requests authorization using:

- `VIEW_MONITORING_METRICS`
- `VIEW_MONITORING_EVENTS`

Allowed roles are:

- `OPERATOR`
- `ADMIN`

No separate RBAC implementation was introduced in the monitoring module.

## Authentication and Authorization

Day 31 verifies authentication:

Unauthenticated request
→ `401 Unauthorized`

Day 32 verifies authorization:

Authenticated USER
→ `403 Forbidden`

Authenticated OPERATOR
→ allowed

Authenticated ADMIN
→ allowed

## Validation

`MonitoringAuthorizationTest` verifies:

- USER cannot view metrics
- USER cannot view events
- OPERATOR can view metrics
- OPERATOR can view events
- ADMIN can view metrics
- ADMIN can view events

Existing monitoring controller, edge-case and authentication tests were
also updated for the controller dependencies.

## Scope

This policy protects monitoring data exposed through the EnvForge
Control API.

It does not introduce independent Entra role configuration for Grafana,
Prometheus or Loki.

## Result

Operational monitoring information is restricted to EnvForge operators
and administrators while regular users are denied access.
