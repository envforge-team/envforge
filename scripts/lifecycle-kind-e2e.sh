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

OWNER_ID="${ENVFORGE_E2E_OWNER_ID:-owner-operator}"
OWNER_EMAIL="${ENVFORGE_E2E_OWNER_EMAIL:-owner@example.test}"
OWNER_ROLE="${ENVFORGE_E2E_OWNER_ROLE:-OPERATOR}"

auth_headers=(
  -H "X-Debug-User-Id: ${OWNER_ID}"
  -H "X-Debug-User-Email: ${OWNER_EMAIL}"
  -H "X-Debug-User-Role: ${OWNER_ROLE}"
)

health_check() {
  local url="$1"
  local name="$2"

  local status
  status="$(
    curl \
      --fail \
      --silent \
      "${url}/actuator/health" \
    | jq -r '.status'
  )"

  if [[ "$status" != "UP" ]]; then
    echo "ERROR: ${name} is not healthy: ${status}"
    exit 1
  fi
}

create_environment() {
  local name="$1"

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
}

wait_for_status() {
  local environment_id="$1"
  local expected="$2"
  local attempts="$3"
  local label="$4"

  for attempt in $(seq 1 "$attempts"); do
    local response
    response="$(
      curl \
        --fail \
        --silent \
        "${auth_headers[@]}" \
        "${API_URL}/api/environments/${environment_id}"
    )"

    local status
    status="$(
      printf '%s' "$response" \
      | jq -r '.status'
    )"

    echo "${label} ${attempt}/${attempts}: ${status}"

    if [[ "$status" == "$expected" ]]; then
      return 0
    fi

    if [[ "$status" == "FAILED" ]]; then
      echo "ERROR: environment entered FAILED."
      printf '%s\n' "$response" | jq .
      return 1
    fi

    sleep 3
  done

  echo "ERROR: environment did not become ${expected}."
  return 1
}

verify_cluster_cleanup() {
  local release_name="$1"
  local namespace_name="$2"

  if kubectl \
      --context "$ADMIN_CONTEXT" \
      get namespace "$namespace_name" >/dev/null 2>&1; then
    echo "ERROR: namespace still exists: ${namespace_name}"
    exit 1
  fi

  if helm \
      --kube-context "$ADMIN_CONTEXT" \
      status "$release_name" \
      --namespace "$namespace_name" >/dev/null 2>&1; then
    echo "ERROR: Helm release still exists: ${release_name}"
    exit 1
  fi
}

db_scalar() {
  local sql="$1"

  (
    cd "$ROOT_DIR"
    docker compose exec -T postgres \
      psql \
      -U envforge \
      -d envforge \
      -At \
      -v ON_ERROR_STOP=1 \
      -c "$sql"
  )
}

echo "=== Health checks ==="
health_check "$API_URL" "control-api"
health_check "$WORKER_URL" "cleanup-worker"
kubectl --context "$ADMIN_CONTEXT" get nodes >/dev/null

echo
echo "=== DELETE through Control API ==="

delete_name="lifecycle-delete-$(date +%s)"
delete_response="$(create_environment "$delete_name")"

delete_id="$(
  printf '%s' "$delete_response" \
  | jq -r '.id'
)"

delete_namespace="$(
  printf '%s' "$delete_response" \
  | jq -r '.namespace'
)"

delete_owner="$(
  printf '%s' "$delete_response" \
  | jq -r '.createdBy'
)"

if [[ "$delete_owner" != "$OWNER_ID" ]]; then
  echo "ERROR: expected createdBy=${OWNER_ID}, got ${delete_owner}"
  exit 1
fi

wait_for_status "$delete_id" READY 40 "READY wait"

delete_job="$(
  curl \
    --fail-with-body \
    --silent \
    --show-error \
    --request POST \
    "${auth_headers[@]}" \
    "${API_URL}/api/environments/${delete_id}/delete"
)"

printf '%s\n' "$delete_job" | jq .

wait_for_status "$delete_id" DELETED 60 "DELETE wait"
verify_cluster_cleanup "$delete_name" "$delete_namespace"

delete_actor="$(
  db_scalar "
    SELECT actor_id
    FROM lifecycle_audit
    WHERE environment_id = '${delete_id}'
      AND action = 'DELETE'
    ORDER BY created_at DESC
    LIMIT 1;
  "
)"

if [[ "$delete_actor" != "$OWNER_EMAIL" ]]; then
  echo "ERROR: DELETE actor should be ${OWNER_EMAIL}, got ${delete_actor}"
  exit 1
fi

echo "DELETE test passed. actor=${delete_actor}"

echo
echo "=== EXPIRATION ==="

expire_name="lifecycle-expire-$(date +%s)"
expire_response="$(create_environment "$expire_name")"

expire_id="$(
  printf '%s' "$expire_response" \
  | jq -r '.id'
)"

expire_namespace="$(
  printf '%s' "$expire_response" \
  | jq -r '.namespace'
)"

wait_for_status "$expire_id" READY 40 "READY wait"

(
  cd "$ROOT_DIR"
  docker compose exec -T postgres \
    psql \
    -U envforge \
    -d envforge \
    -v ON_ERROR_STOP=1 \
    -c "UPDATE environments
        SET expires_at = NOW() - INTERVAL '1 minute',
            updated_at = NOW()
        WHERE id = '${expire_id}';"
)

wait_for_status "$expire_id" DELETED 60 "EXPIRE wait"
verify_cluster_cleanup "$expire_name" "$expire_namespace"

expire_actor="$(
  db_scalar "
    SELECT actor_id
    FROM lifecycle_audit
    WHERE environment_id = '${expire_id}'
      AND action = 'EXPIRE'
    ORDER BY created_at DESC
    LIMIT 1;
  "
)"

if [[ "$expire_actor" != "SYSTEM" ]]; then
  echo "ERROR: EXPIRE actor should be SYSTEM, got ${expire_actor}"
  exit 1
fi

echo "EXPIRATION test passed. actor=${expire_actor}"

echo
echo "Lifecycle Kind end-to-end validation passed."
