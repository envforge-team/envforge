#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

echo "=== Week 8 final validation ==="

echo
echo "1. Bash syntax"

scripts=(
  scripts/kind-check.sh
  scripts/kind-configure-cleanup-worker-rbac.sh
  scripts/run-cleanup-worker-kind.sh
  scripts/lifecycle-kind-e2e.sh
  scripts/lifecycle-rollback-kind-e2e.sh
  scripts/lifecycle-security-e2e.sh
  scripts/lifecycle-demo.sh
  scripts/week7-8-preflight.sh
  scripts/week7-8-e2e-validation.sh
)

for script in "${scripts[@]}"; do
  bash -n "$ROOT_DIR/$script"
  echo "OK $script"
done

echo
echo "2. Helm chart"

helm lint \
  "$ROOT_DIR/deployment/helm/envforge-workload"

echo
echo "3. Control API tests"

(
  cd "$ROOT_DIR/apps/control-api"
  ./mvnw clean test
)

echo
echo "4. Cleanup-worker tests"

(
  cd "$ROOT_DIR/apps/cleanup-worker"
  mvn clean test
)

echo
echo "5. Git whitespace check"

(
  cd "$ROOT_DIR"
  git diff --check
)

echo
echo "Week 8 final validation passed."
