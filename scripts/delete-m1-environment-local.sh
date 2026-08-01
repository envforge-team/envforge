#!/usr/bin/env bash

set -euo pipefail

environment_namespace="${ENVFORGE_NAMESPACE:-env-static-demo-m1}"
release_name="${ENVFORGE_RELEASE:-static-demo-m1}"
expected_context="docker-desktop"

current_context="$(kubectl config current-context)"

if [[ "$current_context" != "$expected_context" ]]; then
  echo "Refusing to continue."
  echo "Expected context: ${expected_context}"
  echo "Current context: ${current_context}"
  exit 1
fi

if ! kubectl get namespace \
  "$environment_namespace" \
  >/dev/null 2>&1; then

  echo "Namespace does not exist: ${environment_namespace}"
  exit 0
fi

managed_label="$(
  kubectl get namespace \
    "$environment_namespace" \
    -o jsonpath='{.metadata.labels.envforge\.io/managed}'
)"

if [[ "$managed_label" != "true" ]]; then
  echo "Refusing to delete an unmanaged namespace."
  echo "Namespace: ${environment_namespace}"
  exit 1
fi

echo "Deleting Helm release, if present..."

if helm status \
  "$release_name" \
  --namespace "$environment_namespace" \
  >/dev/null 2>&1; then

  helm uninstall \
    "$release_name" \
    --namespace "$environment_namespace"
else
  echo "Helm release does not exist: ${release_name}"
fi

echo "Verifying release cleanup..."

remaining_workloads="$(
  kubectl get all \
    --namespace "$environment_namespace" \
    --no-headers 2>/dev/null |
    wc -l
)"

if [[ "$remaining_workloads" -ne 0 ]]; then
  echo "Workload resources remain in the namespace:"
  kubectl get all \
    --namespace "$environment_namespace"
  exit 1
fi

remaining_quota="$(
  kubectl get resourcequota \
    --namespace "$environment_namespace" \
    --no-headers 2>/dev/null |
    wc -l
)"

remaining_limits="$(
  kubectl get limitrange \
    --namespace "$environment_namespace" \
    --no-headers 2>/dev/null |
    wc -l
)"

if [[ "$remaining_quota" -ne 0 ]]; then
  echo "ResourceQuota resources remain."
  exit 1
fi

if [[ "$remaining_limits" -ne 0 ]]; then
  echo "LimitRange resources remain."
  exit 1
fi

echo "Deleting managed namespace..."

kubectl delete namespace "$environment_namespace"

kubectl wait \
  --for=delete \
  "namespace/${environment_namespace}" \
  --timeout=120s

echo
echo "Environment deleted successfully."
echo "Release: ${release_name}"
echo "Namespace: ${environment_namespace}"