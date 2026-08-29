#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

API_URL="${ENVFORGE_API_URL:-http://localhost:8080}"
WORKER_URL="${ENVFORGE_WORKER_URL:-http://localhost:8081}"
ADMIN_CONTEXT="${ENVFORGE_ADMIN_KUBE_CONTEXT:-kind-envforge}"
IMAGE_VERSION="${ENVFORGE_IMAGE_VERSION:-0.2.0}"

OWNER_ID="security-owner"
OWNER_EMAIL="security-owner@example.test"
INTRUDER_ID="security-intruder"
INTRUDER_EMAIL="security-intruder@example.test"
ADMIN_ID="security-admin"
ADMIN_EMAIL="security-admin@example.test"

owner_headers=(
  -H "X-Debug-User-Id: ${OWNER_ID}"
  -H "X-Debug-User-Email: ${OWNER_EMAIL}"
  -H "X-Debug-User-Role: OPERATOR"
)

intruder_headers=(
  -H "X-Debug-User-Id: ${INTRUDER_ID}"
  -H "X-Debug-User-Email: ${INTRUDER_EMAIL}"
  -H "X-Debug-User-Role: OPERATOR"
)

admin_headers=(
  -H "X-Debug-User-Id: ${ADMIN_ID}"
  -H "X-Debug-User-Email: ${ADMIN_EMAIL}"
  -H "X-Debug-User-Role: ADMIN"
)

create_environment() {
  local name="$1"

  curl \
    --fail-with-body \
    --silent \
    --show-error \
    --request POST \
    --header "Content-Type: application/json" \
    "${owner_headers[@]}" \
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
}

wait_status() {
  local environment_id="$1"
  local expected="$2"

  for attempt in $(seq 1 60); do
    status="$(
      curl \
        --fail \
        --silent \
        "${owner_headers[@]}" \
        "${API_URL}/api/environments/${environment_id}" \
      | jq -r '.status'
    )"

    echo "wait ${attempt}/60: ${status}"

    if [[ "$status" == "$expected" ]]; then
      return 0
    fi

    if [[ "$status" == "FAILED" ]]; then
      return 1
    fi

    sleep 3
  done

  return 1
}

echo "=== Health ==="

curl --fail --silent \
  "${API_URL}/actuator/health" \
| jq .

curl --fail --silent \
  "${WORKER_URL}/actuator/health" \
| jq .

echo
echo "=== Worker internal endpoint must reject missing token ==="

internal_status="$(
  curl \
    --silent \
    --output /tmp/envforge-worker-auth-response.txt \
    --write-out '%{http_code}' \
    --request POST \
    --header "Content-Type: application/json" \
    --data '{
      "environmentId": "00000000-0000-0000-0000-000000000001",
      "action": "DELETE",
      "actorId": "forged-user",
      "namespaceName": "env-fake",
      "helmReleaseName": "fake"
    }' \
    "${WORKER_URL}/internal/lifecycle/jobs"
)"

if [[ "$internal_status" != "403" ]]; then
  echo "ERROR: expected worker direct call without token to return 403."
  cat /tmp/envforge-worker-auth-response.txt
  exit 1
fi

echo "Direct worker request correctly rejected with 403."

echo
echo "=== Ownership: non-owner OPERATOR must get 403 ==="

name="security-delete-$(date +%s)"
response="$(create_environment "$name")"
environment_id="$(printf '%s' "$response" | jq -r '.id')"
namespace_name="$(printf '%s' "$response" | jq -r '.namespace')"

created_by="$(printf '%s' "$response" | jq -r '.createdBy')"

if [[ "$created_by" != "$OWNER_ID" ]]; then
  echo "ERROR: createdBy should be ${OWNER_ID}, got ${created_by}"
  exit 1
fi

wait_status "$environment_id" READY

intruder_status="$(
  curl \
    --silent \
    --output /tmp/envforge-intruder-response.txt \
    --write-out '%{http_code}' \
    --request POST \
    "${intruder_headers[@]}" \
    "${API_URL}/api/environments/${environment_id}/delete"
)"

if [[ "$intruder_status" != "403" ]]; then
  echo "ERROR: expected non-owner OPERATOR delete to return 403."
  cat /tmp/envforge-intruder-response.txt
  exit 1
fi

echo "Non-owner OPERATOR correctly rejected."

echo
echo "=== Owner OPERATOR delete must succeed ==="

curl \
  --fail-with-body \
  --silent \
  --show-error \
  --request POST \
  "${owner_headers[@]}" \
  "${API_URL}/api/environments/${environment_id}/delete" \
| jq .

wait_status "$environment_id" DELETED

if kubectl \
    --context "$ADMIN_CONTEXT" \
    get namespace "$namespace_name" >/dev/null 2>&1; then
  echo "ERROR: namespace still exists: ${namespace_name}"
  exit 1
fi

owner_actor="$(
  (
    cd "$ROOT_DIR"
    docker compose exec -T postgres \
      psql -U envforge -d envforge -At \
      -c "SELECT actor_id
          FROM lifecycle_audit
          WHERE environment_id='${environment_id}'
            AND action='DELETE'
          ORDER BY created_at DESC
          LIMIT 1;"
  )
)"

if [[ "$owner_actor" != "$OWNER_EMAIL" ]]; then
  echo "ERROR: expected lifecycle actor ${OWNER_EMAIL}, got ${owner_actor}"
  exit 1
fi

echo "Owner delete passed."

echo
echo "=== ADMIN override must succeed on another owner's environment ==="

admin_name="security-admin-delete-$(date +%s)"
admin_response="$(create_environment "$admin_name")"
admin_environment_id="$(printf '%s' "$admin_response" | jq -r '.id')"

wait_status "$admin_environment_id" READY

curl \
  --fail-with-body \
  --silent \
  --show-error \
  --request POST \
  "${admin_headers[@]}" \
  "${API_URL}/api/environments/${admin_environment_id}/delete" \
| jq .

wait_status "$admin_environment_id" DELETED

admin_actor="$(
  (
    cd "$ROOT_DIR"
    docker compose exec -T postgres \
      psql -U envforge -d envforge -At \
      -c "SELECT actor_id
          FROM lifecycle_audit
          WHERE environment_id='${admin_environment_id}'
            AND action='DELETE'
          ORDER BY created_at DESC
          LIMIT 1;"
  )
)"

if [[ "$admin_actor" != "$ADMIN_EMAIL" ]]; then
  echo "ERROR: expected admin actor ${ADMIN_EMAIL}, got ${admin_actor}"
  exit 1
fi

echo
echo "Lifecycle security end-to-end validation passed."
