#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

API_URL="${ENVFORGE_API_URL:-http://localhost:8080}"
ADMIN_CONTEXT="${ENVFORGE_ADMIN_KUBE_CONTEXT:-kind-envforge}"
IMAGE_VERSION="${ENVFORGE_IMAGE_VERSION:-0.2.0}"

OWNER_ID="${ENVFORGE_E2E_OWNER_ID:-rollback-owner}"
OWNER_EMAIL="${ENVFORGE_E2E_OWNER_EMAIL:-rollback-owner@example.test}"
OWNER_ROLE="${ENVFORGE_E2E_OWNER_ROLE:-OPERATOR}"

auth_headers=(
  -H "X-Debug-User-Id: ${OWNER_ID}"
  -H "X-Debug-User-Email: ${OWNER_EMAIL}"
  -H "X-Debug-User-Role: ${OWNER_ROLE}"
)

name="lifecycle-rollback-$(date +%s)"

response="$(
  curl \
    --fail-with-body \
    --silent \
    --show-error \
    --request POST \
    --header "Content-Type: application/json" \
    "${auth_headers[@]}" \
    --data "{
      \"name\": \"${name}\",
      \"template\": \"STATIC_WEB\",
      \"imageVersion\": \"${IMAGE_VERSION}\",
      \"replicas\": 1,
      \"resourceProfile\": \"SMALL\",
      \"lifetimeHours\": 1,
      \"monitoringEnabled\": true
    }" \
    "${API_URL}/api/environments"
)"

environment_id="$(
  printf '%s' "$response" | jq -r '.id'
)"

namespace_name="$(
  printf '%s' "$response" | jq -r '.namespace'
)"

echo "Waiting for initial deployment..."

status=""

for attempt in $(seq 1 40); do
  status="$(
    curl \
      --fail \
      --silent \
      "${auth_headers[@]}" \
      "${API_URL}/api/environments/${environment_id}" \
    | jq -r '.status'
  )"

  echo "READY wait ${attempt}/40: ${status}"

  if [[ "$status" == "READY" ]]; then
    break
  fi

  if [[ "$status" == "FAILED" ]]; then
    echo "ERROR: provisioning failed."
    exit 1
  fi

  sleep 3
done

if [[ "$status" != "READY" ]]; then
  echo "ERROR: environment did not become READY."
  exit 1
fi

echo "Creating Helm revision 2..."

helm upgrade \
  "$name" \
  "$ROOT_DIR/deployment/helm/envforge-workload" \
  --kube-context "$ADMIN_CONTEXT" \
  --namespace "$namespace_name" \
  --reuse-values \
  --set workload.replicas=2 \
  --wait \
  --timeout 2m

revision_count="$(
  helm history \
    "$name" \
    --kube-context "$ADMIN_CONTEXT" \
    --namespace "$namespace_name" \
    --output json \
  | jq 'length'
)"

if [[ "$revision_count" -lt 2 ]]; then
  echo "ERROR: expected at least two Helm revisions."
  exit 1
fi

echo "Requesting rollback through Control API..."

curl \
  --fail-with-body \
  --silent \
  --show-error \
  --request POST \
  --header "Content-Type: application/json" \
  "${auth_headers[@]}" \
  --data '{
    "targetRevision": 1
  }' \
  "${API_URL}/api/environments/${environment_id}/rollback" \
| jq .

echo "Waiting for rollback revision..."

latest_revision=0
latest_status=""

for attempt in $(seq 1 40); do
  history="$(
    helm history \
      "$name" \
      --kube-context "$ADMIN_CONTEXT" \
      --namespace "$namespace_name" \
      --output json
  )"

  latest_revision="$(
    printf '%s' "$history" \
    | jq '.[-1].revision'
  )"

  latest_status="$(
    printf '%s' "$history" \
    | jq -r '.[-1].status'
  )"

  echo \
    "ROLLBACK wait ${attempt}/40: revision=${latest_revision} status=${latest_status}"

  if [[ "$latest_revision" -ge 3
        && "$latest_status" == "deployed" ]]; then
    break
  fi

  sleep 3
done

if [[ "$latest_revision" -lt 3
      || "$latest_status" != "deployed" ]]; then
  echo "ERROR: rollback was not observed."
  exit 1
fi

environment_status="$(
  curl \
    --fail \
    --silent \
    "${auth_headers[@]}" \
    "${API_URL}/api/environments/${environment_id}" \
  | jq -r '.status'
)"

if [[ "$environment_status" != "READY" ]]; then
  echo \
    "ERROR: expected READY after rollback, got ${environment_status}."
  exit 1
fi

rollback_actor="$(
  (
    cd "$ROOT_DIR"
    docker compose exec -T postgres \
      psql \
      -U envforge \
      -d envforge \
      -At \
      -v ON_ERROR_STOP=1 \
      -c "SELECT actor_id
          FROM lifecycle_audit
          WHERE environment_id = '${environment_id}'
            AND action = 'ROLLBACK'
          ORDER BY created_at DESC
          LIMIT 1;"
  )
)"

if [[ "$rollback_actor" != "$OWNER_EMAIL" ]]; then
  echo \
    "ERROR: ROLLBACK actor should be ${OWNER_EMAIL}, got ${rollback_actor}"
  exit 1
fi

echo
echo "Rollback Kind end-to-end validation passed."
echo "Environment ID: ${environment_id}"
echo "Release:        ${name}"
echo "Namespace:      ${namespace_name}"
echo "Actor:          ${rollback_actor}"
