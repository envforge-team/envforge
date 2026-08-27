#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

API_URL="${ENVFORGE_API_URL:-http://localhost:8080}"
WORKER_URL="${ENVFORGE_WORKER_URL:-http://localhost:8081}"
KUBE_CONTEXT="${ENVFORGE_KUBE_CONTEXT:-kind-envforge}"
IMAGE_VERSION="${ENVFORGE_IMAGE_VERSION:-0.2.0}"

required_commands=(
  curl
  jq
  kubectl
  helm
  docker
)

for command_name in "${required_commands[@]}"; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "ERROR: missing command: ${command_name}"
    exit 1
  fi
done

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

wait_ready() {
  local environment_id="$1"

  for attempt in $(seq 1 40); do
    local response
    response="$(
      curl \
        --fail \
        --silent \
        "${API_URL}/api/environments/${environment_id}"
    )"

    local status
    status="$(printf '%s' "$response" | jq -r '.status')"

    echo "READY wait ${attempt}/40: ${status}"

    case "$status" in
      READY)
        return 0
        ;;
      FAILED|DELETED)
        echo "ERROR: unexpected environment status: ${status}"
        printf '%s\n' "$response" | jq .
        return 1
        ;;
    esac

    sleep 3
  done

  echo "ERROR: environment did not become READY."
  return 1
}

wait_deleted() {
  local environment_id="$1"

  for attempt in $(seq 1 60); do
    local response
    response="$(
      curl \
        --fail \
        --silent \
        "${API_URL}/api/environments/${environment_id}"
    )"

    local status
    status="$(printf '%s' "$response" | jq -r '.status')"

    echo "DELETE wait ${attempt}/60: ${status}"

    case "$status" in
      DELETED)
        return 0
        ;;
      FAILED)
        echo "ERROR: lifecycle operation failed."
        printf '%s\n' "$response" | jq .
        return 1
        ;;
    esac

    sleep 3
  done

  echo "ERROR: environment did not become DELETED."
  return 1
}

create_environment() {
  local name="$1"

  curl \
    --fail-with-body \
    --silent \
    --show-error \
    --request POST \
    --header "Content-Type: application/json" \
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

verify_cluster_cleanup() {
  local release_name="$1"
  local namespace_name="$2"

  if kubectl \
      --context "$KUBE_CONTEXT" \
      get namespace "$namespace_name" >/dev/null 2>&1; then
    echo "ERROR: namespace still exists: ${namespace_name}"
    exit 1
  fi

  if helm \
      --kube-context "$KUBE_CONTEXT" \
      status "$release_name" \
      --namespace "$namespace_name" >/dev/null 2>&1; then
    echo "ERROR: Helm release still exists: ${release_name}"
    exit 1
  fi
}

echo "=== Health checks ==="
health_check "$API_URL" "control-api"
health_check "$WORKER_URL" "cleanup-worker"

current_context="$(kubectl config current-context)"

if [[ "$current_context" != "$KUBE_CONTEXT" ]]; then
  echo "ERROR: expected Kubernetes context ${KUBE_CONTEXT}"
  echo "Current context: ${current_context}"
  exit 1
fi

echo
echo "=== DELETE test ==="

delete_name="lifecycle-delete-$(date +%s)"
delete_response="$(create_environment "$delete_name")"

delete_id="$(printf '%s' "$delete_response" | jq -r '.id')"
delete_namespace="$(printf '%s' "$delete_response" | jq -r '.namespace')"

wait_ready "$delete_id"

curl \
  --fail-with-body \
  --silent \
  --show-error \
  --request POST \
  --header "Content-Type: application/json" \
  --data "{
    \"environmentId\": \"${delete_id}\",
    \"action\": \"DELETE\",
    \"actorId\": \"kind-e2e\",
    \"namespaceName\": \"${delete_namespace}\",
    \"helmReleaseName\": \"${delete_name}\"
  }" \
  "${WORKER_URL}/internal/lifecycle/jobs" \
  | jq .

wait_deleted "$delete_id"
verify_cluster_cleanup "$delete_name" "$delete_namespace"

echo
echo "DELETE test passed."

echo
echo "=== EXPIRATION test ==="

expire_name="lifecycle-expire-$(date +%s)"
expire_response="$(create_environment "$expire_name")"

expire_id="$(printf '%s' "$expire_response" | jq -r '.id')"
expire_namespace="$(printf '%s' "$expire_response" | jq -r '.namespace')"

wait_ready "$expire_id"

docker compose \
  --file "$ROOT_DIR/compose.yaml" \
  exec \
  -T \
  postgres \
  psql \
  -U envforge \
  -d envforge \
  -v ON_ERROR_STOP=1 \
  -c "UPDATE environments
      SET expires_at = NOW() - INTERVAL '1 minute',
          updated_at = NOW()
      WHERE id = '${expire_id}';"

wait_deleted "$expire_id"
verify_cluster_cleanup "$expire_name" "$expire_namespace"

echo
echo "EXPIRATION test passed."
echo
echo "Lifecycle Kind end-to-end validation passed."
