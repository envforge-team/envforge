#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

API_URL="${ENVFORGE_API_URL:-http://localhost:8080}"
WORKER_URL="${ENVFORGE_WORKER_URL:-http://localhost:8081}"
WORKER_CONTEXT="${ENVFORGE_WORKER_KUBE_CONTEXT:-kind-envforge-cleanup-worker}"

echo "=== Application health ==="

api_status="$(
  curl \
    --fail \
    --silent \
    "${API_URL}/actuator/health" \
  | jq -r '.status'
)"

worker_status="$(
  curl \
    --fail \
    --silent \
    "${WORKER_URL}/actuator/health" \
  | jq -r '.status'
)"

echo "control-api:    ${api_status}"
echo "cleanup-worker: ${worker_status}"

[[ "$api_status" == "UP" ]]
[[ "$worker_status" == "UP" ]]

echo
echo "=== Limited worker RBAC ==="

kubectl \
  --context "$WORKER_CONTEXT" \
  auth can-i delete namespaces

if [[ "$(
  kubectl \
    --context "$WORKER_CONTEXT" \
    auth can-i create clusterroles.rbac.authorization.k8s.io
)" != "no" ]]; then
  echo "ERROR: cleanup-worker can create ClusterRoles."
  exit 1
fi

echo "ClusterRole creation correctly denied."

echo
echo "=== Security E2E ==="
"$ROOT_DIR/scripts/lifecycle-security-e2e.sh"

echo
echo "=== Delete + expiration E2E ==="
"$ROOT_DIR/scripts/lifecycle-kind-e2e.sh"

echo
echo "=== Rollback E2E ==="
"$ROOT_DIR/scripts/lifecycle-rollback-kind-e2e.sh"

echo
echo "=== Metrics ==="

if ! curl \
    --fail \
    --silent \
    "${WORKER_URL}/actuator/prometheus" \
  | grep -q "envforge_lifecycle"; then
  echo "ERROR: lifecycle metrics not found."
  exit 1
fi

echo "Lifecycle metrics found."

echo
echo "=== Final lifecycle jobs ==="

(
  cd "$ROOT_DIR"
  docker compose exec -T postgres \
    psql \
    -U envforge \
    -d envforge \
    -c "SELECT action,status,attempt_count,actor_id,last_error
        FROM lifecycle_job
        ORDER BY created_at DESC
        LIMIT 20;"
)

echo
echo "Week 7-8 end-to-end validation passed."
