#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

API_URL="${ENVFORGE_API_URL:-http://localhost:8080}"
ADMIN_CONTEXT="${ENVFORGE_ADMIN_KUBE_CONTEXT:-kind-envforge}"
IMAGE_VERSION="${ENVFORGE_IMAGE_VERSION:-0.2.0}"

USER_ID="${ENVFORGE_DEMO_USER_ID:-demo-operator}"
USER_EMAIL="${ENVFORGE_DEMO_USER_EMAIL:-demo@example.test}"

headers=(
  -H "X-Debug-User-Id: ${USER_ID}"
  -H "X-Debug-User-Email: ${USER_EMAIL}"
  -H "X-Debug-User-Role: OPERATOR"
)

name="lifecycle-demo-$(date +%s)"

echo "1) Creating environment..."

response="$(
  curl \
    --fail-with-body \
    --silent \
    --show-error \
    --request POST \
    --header "Content-Type: application/json" \
    "${headers[@]}" \
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

printf '%s\n' "$response" | jq .

id="$(printf '%s' "$response" | jq -r '.id')"
namespace="$(printf '%s' "$response" | jq -r '.namespace')"

echo
echo "2) Waiting for READY..."

for attempt in $(seq 1 40); do
  status="$(
    curl \
      --fail \
      --silent \
      "${headers[@]}" \
      "${API_URL}/api/environments/${id}" \
    | jq -r '.status'
  )"

  echo "   ${attempt}/40 ${status}"

  [[ "$status" == "READY" ]] && break

  if [[ "$status" == "FAILED" ]]; then
    echo "ERROR: provisioning failed."
    exit 1
  fi

  sleep 3
done

[[ "$status" == "READY" ]] || {
  echo "ERROR: environment did not become READY."
  exit 1
}

echo
echo "3) Kubernetes state before delete:"

kubectl \
  --context "$ADMIN_CONTEXT" \
  get namespace "$namespace" \
  --show-labels

helm \
  --kube-context "$ADMIN_CONTEXT" \
  list \
  --namespace "$namespace"

echo
echo "4) Requesting DELETE through Control API..."

curl \
  --fail-with-body \
  --silent \
  --show-error \
  --request POST \
  "${headers[@]}" \
  "${API_URL}/api/environments/${id}/delete" \
| jq .

echo
echo "5) Watching DB lifecycle job..."

for attempt in $(seq 1 30); do
  (
    cd "$ROOT_DIR"
    docker compose exec -T postgres \
      psql \
      -U envforge \
      -d envforge \
      -c "SELECT action,status,attempt_count,actor_id,last_error
          FROM lifecycle_job
          WHERE environment_id='${id}'
          ORDER BY created_at DESC
          LIMIT 1;"
  )

  status="$(
    curl \
      --fail \
      --silent \
      "${headers[@]}" \
      "${API_URL}/api/environments/${id}" \
    | jq -r '.status'
  )"

  echo "Environment status: ${status}"

  [[ "$status" == "DELETED" ]] && break

  sleep 3
done

[[ "$status" == "DELETED" ]] || {
  echo "ERROR: environment did not become DELETED."
  exit 1
}

echo
echo "6) Kubernetes state after delete:"

if kubectl \
    --context "$ADMIN_CONTEXT" \
    get namespace "$namespace" >/dev/null 2>&1; then
  echo "ERROR: namespace still exists."
  exit 1
else
  echo "Namespace removed: ${namespace}"
fi

echo
echo "7) Lifecycle audit:"

(
  cd "$ROOT_DIR"
  docker compose exec -T postgres \
    psql \
    -U envforge \
    -d envforge \
    -c "SELECT actor_id,action,result,details,created_at
        FROM lifecycle_audit
        WHERE environment_id='${id}'
        ORDER BY created_at;"
)

echo
echo "Lifecycle DELETE demo completed successfully."
