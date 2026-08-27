# Day 36 - Monitoring Bugfix

## Goal

Fix a reproducibility issue in the EnvForge incident alert end-to-end
validation.

## Problem

The Day 35 E2E validator depended on manual setup previously performed
during Day 33.

The script expected the Kubernetes Secret:

`reliability-demo-incident-admin`

to already exist.

It also expected the `reliability-demo-api` Deployment to already expose:

`ENVFORGE_INCIDENT_ADMIN_KEY`

from that Secret.

This meant that the incident alert test worked in the existing local
cluster but could fail on a fresh Kind environment or another developer
machine.

## Fix

`observability/scripts/validate-incident-alert-e2e.sh` now prepares its
runtime incident administration access automatically.

If the incident Secret does not exist, the validator:

- generates a cryptographically random key
- creates the Kubernetes Secret at runtime
- does not print the secret value

If the reliability deployment does not reference the Secret, the
validator:

- injects the Secret using `kubectl set env`
- waits for the deployment rollout to complete

If the Secret and deployment configuration already exist, they are
reused.

## Validation

The existing manual setup was intentionally removed before testing:

- `ENVFORGE_INCIDENT_ADMIN_KEY` was removed from the Deployment
- `reliability-demo-incident-admin` was deleted

The Day 35 E2E validator was then executed again.

The validator successfully:

- created the runtime-only incident Secret
- injected the Secret into the reliability Deployment
- triggered the controlled 5xx incident
- detected the elevated Prometheus 5xx ratio
- observed the reliability alert
- reset the incident
- verified workload recovery
- verified alert resolution

The standard Kind observability validation also passed afterward.

## Secret Handling

The generated incident administration key is runtime-only.

The key value is:

- not printed
- not committed to Git
- not stored in repository manifests

## Result

The incident alert E2E validation is now self-contained and reproducible
on a correctly deployed Kind environment without requiring the manual
Day 33 secret setup.
