# Day 31 - Protect Monitoring Endpoints

## Goal

Verify that the EnvForge monitoring metrics and events endpoints are
protected by the existing production authentication configuration.

## Protected Endpoints

The monitoring API exposes:

- `GET /api/environments/{environmentId}/monitoring/metrics`
- `GET /api/environments/{environmentId}/monitoring/events`

## Existing Security Integration

EnvForge already provides two Spring Security configurations.

The default `!entra` profile is intentionally permissive for local
development and tests.

The `entra` profile enables the OAuth2 resource server and requires
authentication for application API requests while keeping only health
and info actuator endpoints public.

The monitoring module reuses this existing security boundary rather
than introducing a separate authentication implementation.

## Validation

`MonitoringSecurityTest` runs with the `entra` profile and imports the
existing `EntraSecurityConfig`.

The test verifies:

- metrics without a bearer token returns `401 Unauthorized`
- events without a bearer token returns `401 Unauthorized`
- metrics with a valid decoded JWT passes authentication
- events with a valid decoded JWT passes authentication

Result:

- 4 tests
- 0 failures
- 0 errors

## Scope

Day 31 verifies authentication only.

Role-based authorization determining which EnvForge roles may access
monitoring information is handled separately in Day 32.

## Result

Monitoring metrics and events are protected by the existing Entra
JWT authentication path without duplicating the platform security
implementation.
