# Day 33 - Incident and Traffic Generator Security

## Goal

Restrict the EnvForge fault-injection controls and harden the
traffic-generator workload used by the reliability monitoring stack.

## Incident Administration

The reliability demo application exposes controlled incident endpoints:

- POST /admin/incidents/failure
- POST /admin/incidents/latency
- POST /admin/incidents/cpu
- POST /admin/incidents/reset
- GET /admin/incidents/status

These endpoints are protected using the dedicated header:

`X-EnvForge-Incident-Key`

The expected key is provided at runtime through:

`ENVFORGE_INCIDENT_ADMIN_KEY`

The key is not stored in Git.

The implementation is fail-closed:

- missing server key -> access denied
- missing request key -> HTTP 403
- incorrect request key -> HTTP 403
- correct request key -> request allowed

Key comparison uses `MessageDigest.isEqual`.

## Public Reliability Endpoint

The normal workload endpoint remains available:

- GET /work

Incident administration protection does not block generated workload
traffic.

## Traffic Generator Hardening

The traffic-generator remains an internal Kubernetes Deployment.

It does not expose a Kubernetes Service.

The workload is hardened with:

- non-root execution
- privilege escalation disabled
- Linux capabilities dropped
- Kubernetes ServiceAccount token automount disabled
- read-only root filesystem

This reduces the privileges available to the synthetic traffic workload.

## Local Validation

The reliability demo image was tested locally using Docker.

Validated behavior:

- GET /work -> HTTP 200
- incident endpoint without key -> HTTP 403
- incident endpoint with incorrect key -> HTTP 403
- incident endpoint with correct key -> HTTP 200
- enabled failure incident causes GET /work -> HTTP 500
- reset restores GET /work -> HTTP 200

## Kind Validation

A runtime-only Kubernetes Secret was created for the incident
administration key.

The updated reliability demo image was loaded into Kind and the
deployment received the secret through an environment variable.

Validation confirmed:

- incident administration without key -> HTTP 403
- incident administration with incorrect key -> HTTP 403
- incident administration with correct key -> HTTP 200
- GET /work remains available
- traffic-generator has no Kubernetes Service
- ServiceAccount token automount is disabled
- root filesystem is read-only
- complete observability validation passes

## Secret Handling

The incident administration key is created dynamically for the local
Kind environment.

No secret value or Kubernetes Secret manifest containing the value is
committed to the repository.

## Result

Fault-injection controls are no longer anonymously accessible and the
synthetic traffic workload operates with reduced Kubernetes privileges.
