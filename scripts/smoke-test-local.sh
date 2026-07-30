#!/usr/bin/env bash

set -euo pipefail

envforge_api_url="${ENVFORGE_API_URL:-http://localhost:8080}"
environment_name="e2e-m1-$(date +%s)"

echo "Checking Control API health..."

health_status="$(
  curl --fail --silent --show-error \
    "${envforge_api_url}/api/health" |
    jq -r '.status'
)"

if [[ "$health_status" != "UP" ]]; then
  echo "Control API is not healthy."
  exit 1
fi

echo "Checking environment templates..."

template_count="$(
  curl --fail --silent --show-error \
    "${envforge_api_url}/api/templates" |
    jq 'length'
)"

if [[ "$template_count" -lt 1 ]]; then
  echo "No active environment templates found."
  exit 1
fi

echo "Creating environment: ${environment_name}"

create_response="$(
  curl --fail --silent --show-error \
    --request POST \
    "${envforge_api_url}/api/environments" \
    --header "Content-Type: application/json" \
    --data "{
      \"name\": \"${environment_name}\",
      \"template\": \"STATIC_WEB\",
      \"imageVersion\": \"0.1.0\",
      \"replicas\": 2,
      \"resourceProfile\": \"SMALL\",
      \"lifetimeHours\": 4,
      \"monitoringEnabled\": true
    }"
)"

environment_id="$(
  printf '%s' "$create_response" |
    jq -r '.id'
)"

environment_status="$(
  printf '%s' "$create_response" |
    jq -r '.status'
)"

environment_namespace="$(
  printf '%s' "$create_response" |
    jq -r '.namespace'
)"

if [[ -z "$environment_id" || "$environment_id" == "null" ]]; then
  echo "The API did not return an environment ID."
  exit 1
fi

if [[ "$environment_status" != "REQUESTED" ]]; then
  echo "Unexpected status: ${environment_status}"
  exit 1
fi

if [[ "$environment_namespace" != "env-${environment_name}" ]]; then
  echo "Unexpected namespace: ${environment_namespace}"
  exit 1
fi

echo "Reading created environment..."

details_response="$(
  curl --fail --silent --show-error \
    "${envforge_api_url}/api/environments/${environment_id}"
)"

details_name="$(
  printf '%s' "$details_response" |
    jq -r '.name'
)"

if [[ "$details_name" != "$environment_name" ]]; then
  echo "Environment details do not match the request."
  exit 1
fi

echo "Checking environment list..."

list_count="$(
  curl --fail --silent --show-error \
    "${envforge_api_url}/api/environments" |
    jq \
      --arg name "$environment_name" \
      '[.[] | select(.name == $name)] | length'
)"

if [[ "$list_count" -ne 1 ]]; then
  echo "Created environment was not found exactly once."
  exit 1
fi

echo
echo "Smoke test passed."
echo "Environment ID: ${environment_id}"
echo "Environment name: ${environment_name}"
echo "Namespace: ${environment_namespace}"
echo "Status: ${environment_status}"