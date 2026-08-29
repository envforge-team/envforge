#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
  pwd
)"

ADMIN_CONTEXT="${ENVFORGE_ADMIN_KUBE_CONTEXT:-kind-envforge}"
WORKER_CONTEXT="${ENVFORGE_WORKER_KUBE_CONTEXT:-kind-envforge-cleanup-worker}"
WORKER_USER="${ENVFORGE_WORKER_KUBE_USER:-envforge-cleanup-worker}"
SERVICE_ACCOUNT="envforge-cleanup-worker"
SERVICE_ACCOUNT_NAMESPACE="envforge-system"
CLUSTER_ENTRY="${ENVFORGE_KIND_CLUSTER_ENTRY:-kind-envforge}"

required_commands=(kubectl)

for command_name in "${required_commands[@]}"; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "ERROR: missing command: ${command_name}"
    exit 1
  fi
done

echo "Checking admin context..."
kubectl --context "$ADMIN_CONTEXT" get nodes >/dev/null

echo "Applying cleanup-worker RBAC..."
kubectl \
  --context "$ADMIN_CONTEXT" \
  apply \
  -f "$ROOT_DIR/deployment/kubernetes/local/cleanup-worker-rbac.yaml"

echo "Creating a short-lived service-account token..."
token="$(
  kubectl \
    --context "$ADMIN_CONTEXT" \
    --namespace "$SERVICE_ACCOUNT_NAMESPACE" \
    create token "$SERVICE_ACCOUNT" \
    --duration=24h
)"

if [[ -z "$token" ]]; then
  echo "ERROR: service-account token is empty"
  exit 1
fi

kubectl config set-credentials \
  "$WORKER_USER" \
  --token="$token" >/dev/null

kubectl config set-context \
  "$WORKER_CONTEXT" \
  --cluster="$CLUSTER_ENTRY" \
  --user="$WORKER_USER" \
  --namespace="$SERVICE_ACCOUNT_NAMESPACE" >/dev/null

echo
echo "RBAC verification"

can_delete_namespaces="$(
  kubectl \
    --context "$WORKER_CONTEXT" \
    auth can-i delete namespaces
)"

can_read_secrets="$(
  kubectl \
    --context "$WORKER_CONTEXT" \
    auth can-i list secrets --all-namespaces
)"

can_create_clusterroles="$(
  kubectl \
    --context "$WORKER_CONTEXT" \
    auth can-i create clusterroles.rbac.authorization.k8s.io \
    || true
)"

can_delete_nodes="$(
  kubectl \
    --context "$WORKER_CONTEXT" \
    auth can-i delete nodes \
    || true
)"

printf 'delete namespaces:   %s\n' "$can_delete_namespaces"
printf 'list secrets:        %s\n' "$can_read_secrets"
printf 'create clusterroles: %s\n' "$can_create_clusterroles"
printf 'delete nodes:        %s\n' "$can_delete_nodes"

if [[ "$can_delete_namespaces" != "yes" ]]; then
  echo "ERROR: worker must be able to delete managed namespaces"
  exit 1
fi

if [[ "$can_read_secrets" != "yes" ]]; then
  echo "ERROR: worker must be able to read Helm release secrets"
  exit 1
fi

if [[ "$can_create_clusterroles" != "no" ]]; then
  echo "ERROR: worker must NOT be able to create ClusterRoles"
  exit 1
fi

if [[ "$can_delete_nodes" != "no" ]]; then
  echo "ERROR: worker must NOT be able to delete nodes"
  exit 1
fi

echo
echo "Cleanup-worker limited context is ready:"
echo "  ${WORKER_CONTEXT}"
echo
echo "The token expires in 24h. Re-run this script when needed."
