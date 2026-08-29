#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

ADMIN_CONTEXT="${ENVFORGE_ADMIN_KUBE_CONTEXT:-kind-envforge}"

required_commands=(
  docker
  kind
  kubectl
  helm
  java
  mvn
  jq
  curl
  git
)

echo "=== Tool check ==="

for command_name in "${required_commands[@]}"; do
  printf '%-10s ' "$command_name"

  if command -v "$command_name" >/dev/null 2>&1; then
    command -v "$command_name"
  else
    echo "MISSING"
    exit 1
  fi
done

echo
echo "=== Docker ==="

docker info >/dev/null
echo "Docker OK"

echo
echo "=== Memory ==="

free -h

available_kb="$(
  awk '/MemAvailable:/ {print $2}' /proc/meminfo
)"

if [[ "$available_kb" -lt 2097152 ]]; then
  echo
  echo "ERROR: less than 2 GiB RAM is available in WSL."
  echo "Run 'wsl --shutdown' from Windows PowerShell, then retry."
  exit 1
fi

echo "Memory OK"

echo
echo "=== kind ==="

if ! kind get clusters | grep -qx "envforge"; then
  echo "ERROR: kind cluster 'envforge' does not exist."
  exit 1
fi

kubectl --context "$ADMIN_CONTEXT" get nodes

not_ready="$(
  kubectl \
    --context "$ADMIN_CONTEXT" \
    get nodes \
    --no-headers \
  | awk '$2 != "Ready" {count++} END {print count+0}'
)"

if [[ "$not_ready" -ne 0 ]]; then
  echo "ERROR: at least one kind node is not Ready."
  exit 1
fi

echo
echo "=== Lifecycle source checks ==="

grep -q "@EnableScheduling" \
  "$ROOT_DIR/apps/cleanup-worker/src/main/java/com/envforge/cleanupworker/CleanupWorkerApplication.java"

grep -q "verifyManagedNamespace" \
  "$ROOT_DIR/apps/cleanup-worker/src/main/java/com/envforge/cleanupworker/runner/ProcessLifecycleCommandRunner.java"

grep -q "verifyNamespaceDeleted" \
  "$ROOT_DIR/apps/cleanup-worker/src/main/java/com/envforge/cleanupworker/runner/ProcessLifecycleCommandRunner.java"

grep -q '"SYSTEM"' \
  "$ROOT_DIR/apps/cleanup-worker/src/main/java/com/envforge/cleanupworker/service/EnvironmentExpirationService.java"

test -f \
  "$ROOT_DIR/deployment/kubernetes/local/cleanup-worker-rbac.yaml"

echo "Lifecycle source checks OK"

echo
echo "=== Script syntax ==="

for script in \
  "$ROOT_DIR/scripts/kind-configure-cleanup-worker-rbac.sh" \
  "$ROOT_DIR/scripts/run-cleanup-worker-kind.sh" \
  "$ROOT_DIR/scripts/lifecycle-kind-e2e.sh" \
  "$ROOT_DIR/scripts/lifecycle-rollback-kind-e2e.sh" \
  "$ROOT_DIR/scripts/lifecycle-security-e2e.sh" \
  "$ROOT_DIR/scripts/lifecycle-demo.sh" \
  "$ROOT_DIR/scripts/week8-final-validation.sh" \
  "$ROOT_DIR/scripts/week7-8-e2e-validation.sh"
do
  bash -n "$script"
done

echo "Script syntax OK"

echo
echo "Week 7-8 preflight passed."
