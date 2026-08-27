#!/usr/bin/env bash

set -Eeuo pipefail

envforge_api_url="${ENVFORGE_API_URL:-http://localhost:8080}"
prometheus_url="${ENVFORGE_PROMETHEUS_URL:-http://localhost:9090}"
kube_context="${ENVFORGE_KUBE_CONTEXT:-kind-envforge}"
image_version="${ENVFORGE_IMAGE_VERSION:-0.2.0}"
environment_name="${ENVFORGE_ENVIRONMENT_NAME:-e2e-m1-$(date +%s)}"

required_commands=(
  curl
  jq
  kubectl
  helm
)

for required_command in "${required_commands[@]}"; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    echo "Required command is unavailable: ${required_command}"
    exit 1
  fi
done

echo "Checking Kubernetes context..."

current_context="$(kubectl config current-context)"

if [[ "$current_context" != "$kube_context" ]]; then
  echo "Unexpected Kubernetes context."
  echo "Expected: ${kube_context}"
  echo "Current:  ${current_context}"
  exit 1
fi

echo "Checking Kubernetes node..."

kubectl \
  --context "$kube_context" \
  wait \
  --for=condition=Ready \
  nodes \
  --all \
  --timeout=60s

echo "Checking Control API health..."

health_status="$(
  curl \
    --fail \
    --silent \
    --show-error \
    "${envforge_api_url}/actuator/health" \
  | jq -r '.status'
)"

if [[ "$health_status" != "UP" ]]; then
  echo "Control API is not healthy: ${health_status}"
  exit 1
fi

echo "Checking environment templates..."

template_count="$(
  curl \
    --fail \
    --silent \
    --show-error \
    "${envforge_api_url}/api/templates" \
  | jq 'length'
)"

if [[ "$template_count" -lt 1 ]]; then
  echo "No active environment templates were found."
  exit 1
fi

echo "Checking Prometheus readiness..."

curl \
  --fail \
  --silent \
  --show-error \
  "${prometheus_url}/-/ready" \
  >/dev/null

echo "Creating environment: ${environment_name}"

create_response="$(
  curl \
    --fail-with-body \
    --silent \
    --show-error \
    --request POST \
    --header "Content-Type: application/json" \
    --data "{
      \"name\": \"${environment_name}\",
      \"template\": \"STATIC_WEB\",
      \"imageVersion\": \"${image_version}\",
      \"replicas\": 1,
      \"resourceProfile\": \"SMALL\",
      \"lifetimeHours\": 2,
      \"monitoringEnabled\": true
    }" \
    "${envforge_api_url}/api/environments"
)"

printf '%s\n' "$create_response" | jq .

environment_id="$(
  printf '%s' "$create_response" \
  | jq -r '.id'
)"

environment_namespace="$(
  printf '%s' "$create_response" \
  | jq -r '.namespace'
)"

initial_status="$(
  printf '%s' "$create_response" \
  | jq -r '.status'
)"

if [[ -z "$environment_id" || "$environment_id" == "null" ]]; then
  echo "Control API did not return an environment ID."
  exit 1
fi

if [[ "$environment_namespace" != "env-${environment_name}" ]]; then
  echo "Unexpected namespace: ${environment_namespace}"
  exit 1
fi

case "$initial_status" in
  REQUESTED|PROVISIONING|READY)
    ;;
  *)
    echo "Unexpected initial status: ${initial_status}"
    exit 1
    ;;
esac

echo "Waiting for environment to become READY..."

environment_status="$initial_status"
details_response="$create_response"

for attempt in $(seq 1 30); do
  details_response="$(
    curl \
      --fail \
      --silent \
      --show-error \
      "${envforge_api_url}/api/environments/${environment_id}"
  )"

  environment_status="$(
    printf '%s' "$details_response" \
    | jq -r '.status'
  )"

  echo "Attempt ${attempt}/30: status=${environment_status}"

  case "$environment_status" in
    READY)
      break
      ;;
    FAILED|DELETE_FAILED)
      echo "Provisioning failed."
      printf '%s\n' "$details_response" | jq .
      exit 1
      ;;
  esac

  sleep 5
done

if [[ "$environment_status" != "READY" ]]; then
  echo "Environment did not become READY within 150 seconds."
  printf '%s\n' "$details_response" | jq .
  exit 1
fi

echo "Checking environment list..."

list_count="$(
  curl \
    --fail \
    --silent \
    --show-error \
    "${envforge_api_url}/api/environments" \
  | jq \
      --arg environment_name "$environment_name" \
      '[.[] | select(.name == $environment_name)] | length'
)"

if [[ "$list_count" -ne 1 ]]; then
  echo "Environment was not found exactly once."
  exit 1
fi

echo "Checking Kubernetes namespace..."

kubectl \
  --context "$kube_context" \
  get namespace \
  "$environment_namespace"

managed_label="$(
  kubectl \
    --context "$kube_context" \
    get namespace \
    "$environment_namespace" \
    --output jsonpath='{.metadata.labels.envforge\.io/managed}'
)"

if [[ "$managed_label" != "true" ]]; then
  echo "Namespace is missing the EnvForge managed label."
  exit 1
fi

echo "Checking Helm release..."

helm \
  --kube-context "$kube_context" \
  status \
  "$environment_name" \
  --namespace "$environment_namespace"

echo "Waiting for the workload deployment..."

kubectl \
  --context "$kube_context" \
  --namespace "$environment_namespace" \
  wait \
  --for=condition=Available \
  deployment \
  --all \
  --timeout=120s

echo "Checking provisioned resources..."

kubectl \
  --context "$kube_context" \
  --namespace "$environment_namespace" \
  get \
  pods,services,resourcequotas,limitranges

echo "Waiting for Prometheus provisioning metric..."

metric_found="false"

for attempt in $(seq 1 12); do
  prometheus_response="$(
    curl \
      --fail \
      --silent \
      --show-error \
      --get \
      --data-urlencode \
      'query=envforge_provisioning_attempts_total{outcome="success"}' \
      "${prometheus_url}/api/v1/query"
  )"

  metric_series_count="$(
    printf '%s' "$prometheus_response" \
    | jq \
        '[.data.result[]
          | select((.value[1] | tonumber) >= 1)]
         | length'
  )"

  if [[ "$metric_series_count" -ge 1 ]]; then
    metric_found="true"
    break
  fi

  echo "Metric not available yet. Attempt ${attempt}/12."
  sleep 5
done

if [[ "$metric_found" != "true" ]]; then
  echo "Prometheus did not collect the successful provisioning metric."
  exit 1
fi

echo
echo "Local end-to-end test passed."
echo "Environment ID:        ${environment_id}"
echo "Environment name:      ${environment_name}"
echo "Namespace:             ${environment_namespace}"
echo "Status:                ${environment_status}"
echo "Kubernetes context:    ${kube_context}"
echo "Image version:         ${image_version}"
echo "Prometheus validation: passed"