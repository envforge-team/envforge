#!/usr/bin/env bash

set -euo pipefail

environment_namespace="${ENVFORGE_NAMESPACE:-env-static-demo-m1}"
release_name="${ENVFORGE_RELEASE:-static-demo-m1}"
chart_path="deployment/helm/envforge-workload"
values_path="${ENVFORGE_VALUES:-deployment/helm/envforge-workload/values-m1-test.yaml}"
namespace_manifest="deployment/kubernetes/m1-provisioning/namespace.yaml"

current_context="$(kubectl config current-context)"

if [[ "$current_context" != "kind-envforge" ]]; then
  echo "Refusing to continue."
  echo "Expected Kubernetes context: kind-envforge"
  echo "Current Kubernetes context: ${current_context}"
  exit 1
fi

echo "Validating Helm chart..."

helm lint \
  "$chart_path" \
  --values "$values_path"

echo "Creating environment namespace..."

kubectl apply \
  --filename "$namespace_manifest"

echo "Installing EnvForge release..."

helm upgrade \
  --install \
  "$release_name" \
  "$chart_path" \
  --namespace "$environment_namespace" \
  --values "$values_path" \
  --wait \
  --timeout 2m

echo "Verifying release..."

helm status \
  "$release_name" \
  --namespace "$environment_namespace"

echo
echo "Environment policies installed successfully."
echo "Release: ${release_name}"
echo "Namespace: ${environment_namespace}"