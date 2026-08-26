#!/usr/bin/env bash
set -Eeuo pipefail

KIND_CLUSTER="${KIND_CLUSTER:-envforge}"
KIND_CONTEXT="kind-${KIND_CLUSTER}"

required_commands=(
  docker
  kind
  kubectl
  helm
)

for command_name in "${required_commands[@]}"; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "ERROR: missing command: ${command_name}"
    exit 1
  fi
done

echo "=== Docker ==="
docker version --format 'Client={{.Client.Version}} Server={{.Server.Version}}'

echo
echo "=== Kind clusters ==="
kind get clusters

if ! kind get clusters | grep -Fxq "$KIND_CLUSTER"; then
  echo "ERROR: Kind cluster does not exist: ${KIND_CLUSTER}"
  exit 1
fi

echo
echo "=== Kubernetes context ==="
kubectl config use-context "$KIND_CONTEXT"
kubectl cluster-info --context "$KIND_CONTEXT"

echo
echo "=== Nodes ==="
kubectl --context "$KIND_CONTEXT" get nodes -o wide

kubectl \
  --context "$KIND_CONTEXT" \
  wait \
  --for=condition=Ready \
  nodes \
  --all \
  --timeout=120s

echo
echo "=== Helm ==="
helm version --short

echo
echo "Local Kind environment is ready."
