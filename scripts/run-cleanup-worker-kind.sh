#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

KIND_CONTEXT="${ENVFORGE_KUBE_CONTEXT:-kind-envforge-cleanup-worker}"
INTERNAL_TOKEN="${ENVFORGE_LIFECYCLE_INTERNAL_TOKEN:-local-dev-internal-token}"

required_commands=(
  docker
  kubectl
  helm
  mvn
)

for command_name in "${required_commands[@]}"; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "ERROR: missing command: ${command_name}"
    exit 1
  fi
done

if ! kubectl \
    --context "$KIND_CONTEXT" \
    get --raw=/version >/dev/null 2>&1; then
  echo "ERROR: Kubernetes context is not usable: ${KIND_CONTEXT}"
  echo "Run: ./scripts/kind-configure-cleanup-worker-rbac.sh"
  exit 1
fi

cd "$ROOT_DIR"

echo "Starting local PostgreSQL if necessary..."
docker compose up -d postgres

echo "Waiting for PostgreSQL..."

for attempt in $(seq 1 30); do
  if docker compose exec -T postgres \
      pg_isready -U envforge -d envforge >/dev/null 2>&1; then
    echo "PostgreSQL is ready."
    break
  fi

  if [[ "$attempt" -eq 30 ]]; then
    echo "ERROR: PostgreSQL did not become ready."
    exit 1
  fi

  sleep 2
done

cd "$ROOT_DIR/apps/cleanup-worker"

echo
echo "Starting cleanup-worker"
echo "Kubernetes context: ${KIND_CONTEXT}"
echo "Lifecycle runner: real"
echo "Lifecycle scheduler: enabled"
echo "Scheduler delay: 10000 ms"
echo

ENVFORGE_LIFECYCLE_RUNNER_MODE=real \
ENVFORGE_LIFECYCLE_SCHEDULER_ENABLED=true \
ENVFORGE_LIFECYCLE_SCHEDULER_DELAY_MILLISECONDS=10000 \
ENVFORGE_LIFECYCLE_INTERNAL_TOKEN="$INTERNAL_TOKEN" \
ENVFORGE_KUBE_CONTEXT="$KIND_CONTEXT" \
DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/envforge}" \
DB_USERNAME="${DB_USERNAME:-envforge}" \
DB_PASSWORD="${DB_PASSWORD:-envforge-local-password}" \
mvn spring-boot:run
