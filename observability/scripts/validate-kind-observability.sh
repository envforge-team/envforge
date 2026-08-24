#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONTEXT="${ENVFORGE_KUBE_CONTEXT:-kind-envforge}"
APP_NAMESPACE="env-reliability-demo"
MONITORING_NAMESPACE="monitoring"

PROM_PORT=19092
LOKI_PORT=13100
GRAFANA_PORT=13001

PROM_PF=""
LOKI_PF=""
GRAFANA_PF=""

cleanup() {
  [ -n "${PROM_PF}" ] && kill "${PROM_PF}" 2>/dev/null || true
  [ -n "${LOKI_PF}" ] && kill "${LOKI_PF}" 2>/dev/null || true
  [ -n "${GRAFANA_PF}" ] && kill "${GRAFANA_PF}" 2>/dev/null || true
}

trap cleanup EXIT

fail() {
  echo "[FAIL] $1"
  exit 1
}

ok() {
  echo "[OK] $1"
}

wait_http() {
  local url="$1"
  local name="$2"

  for _ in $(seq 1 30); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      ok "${name} reachable"
      return 0
    fi

    sleep 1
  done

  fail "${name} did not become reachable"
}

echo "=== EnvForge Kind Observability Validation ==="

CURRENT_CONTEXT="$(kubectl config current-context)"

if [ "${CURRENT_CONTEXT}" != "${EXPECTED_CONTEXT}" ]; then
  fail "Expected context ${EXPECTED_CONTEXT}, got ${CURRENT_CONTEXT}"
fi

ok "Kubernetes context is ${EXPECTED_CONTEXT}"

echo
echo "=== Kubernetes workloads ==="

kubectl wait \
  --namespace "${APP_NAMESPACE}" \
  --for=condition=available \
  deployment/reliability-demo-api \
  --timeout=60s

kubectl wait \
  --namespace "${APP_NAMESPACE}" \
  --for=condition=available \
  deployment/traffic-generator \
  --timeout=60s

ok "Reliability Demo API is available"
ok "Traffic generator is available"

kubectl get servicemonitor reliability-demo-api \
  --namespace "${APP_NAMESPACE}" \
  >/dev/null

ok "ServiceMonitor exists"

ENDPOINT_COUNT="$(
  kubectl get endpointslice \
    --namespace "${APP_NAMESPACE}" \
    -l kubernetes.io/service-name=reliability-demo-api-metrics \
    -o json |
  python3 -c '
import json
import sys

data = json.load(sys.stdin)

count = 0
for item in data.get("items", []):
    for endpoint in item.get("endpoints", []):
        count += len(endpoint.get("addresses", []))

print(count)
'
)"

if [ "${ENDPOINT_COUNT}" -lt 2 ]; then
  fail "Expected at least 2 metrics endpoints, found ${ENDPOINT_COUNT}"
fi

ok "Metrics service has ${ENDPOINT_COUNT} pod endpoints"

echo
echo "=== Starting local validation tunnels ==="

kubectl \
  --namespace "${MONITORING_NAMESPACE}" \
  port-forward \
  svc/kube-prometheus-stack-prometheus \
  "${PROM_PORT}:9090" \
  >/tmp/envforge-prometheus-port-forward.log 2>&1 &

PROM_PF=$!

kubectl \
  --namespace "${MONITORING_NAMESPACE}" \
  port-forward \
  svc/loki-gateway \
  "${LOKI_PORT}:80" \
  >/tmp/envforge-loki-port-forward.log 2>&1 &

LOKI_PF=$!

kubectl \
  --namespace "${MONITORING_NAMESPACE}" \
  port-forward \
  svc/kube-prometheus-stack-grafana \
  "${GRAFANA_PORT}:80" \
  >/tmp/envforge-grafana-port-forward.log 2>&1 &

GRAFANA_PF=$!

wait_http \
  "http://localhost:${PROM_PORT}/-/ready" \
  "Prometheus"

wait_http \
  "http://localhost:${LOKI_PORT}/loki/api/v1/labels" \
  "Loki"

wait_http \
  "http://localhost:${GRAFANA_PORT}/api/health" \
  "Grafana"

echo
echo "=== Prometheus validation ==="

PROM_UP="$(
  curl -GsS \
    "http://localhost:${PROM_PORT}/api/v1/query" \
    --data-urlencode \
    'query=up{service="reliability-demo-api-metrics"}'
)"

printf '%s' "${PROM_UP}" |
python3 -c '
import json
import sys

data = json.load(sys.stdin)
results = data["data"]["result"]

if len(results) < 2:
    raise SystemExit(
        f"Expected at least 2 Prometheus targets, got {len(results)}"
    )

bad = [
    item
    for item in results
    if float(item["value"][1]) != 1.0
]

if bad:
    raise SystemExit("One or more Prometheus targets are not UP")

print(f"[OK] Prometheus reports {len(results)} workload targets UP")
'

HTTP_METRICS="$(
  curl -GsS \
    "http://localhost:${PROM_PORT}/api/v1/query" \
    --data-urlencode \
    'query=sum(http_server_requests_seconds_count{namespace="env-reliability-demo",uri="/work"})'
)"

printf '%s' "${HTTP_METRICS}" |
python3 -c '
import json
import sys

data = json.load(sys.stdin)
results = data["data"]["result"]

if not results:
    raise SystemExit("No /work request metric found")

value = float(results[0]["value"][1])

if value <= 0:
    raise SystemExit("/work request count is not greater than zero")

print(f"[OK] Prometheus /work request count = {value:g}")
'

echo
echo "=== Prometheus recording rules ==="

RECORDED_TARGETS="$(
  curl -GsS \
    "http://localhost:${PROM_PORT}/api/v1/query" \
    --data-urlencode \
    'query=envforge_reliability:targets_up'
)"

printf '%s' "${RECORDED_TARGETS}" |
python3 -c '
import json
import sys

data = json.load(sys.stdin)
results = data["data"]["result"]

if not results:
    raise SystemExit("Recording rule targets_up returned no result")

value = float(results[0]["value"][1])

if value < 2:
    raise SystemExit(
        f"Expected at least 2 recorded targets UP, got {value:g}"
    )

print(f"[OK] Recording rule reports {value:g} workload targets UP")
'

echo
echo "=== Loki validation ==="

LOKI_RESULT="$(
  curl -GsS \
    "http://localhost:${LOKI_PORT}/loki/api/v1/query_range" \
    --data-urlencode \
    'query={namespace="env-reliability-demo",container="traffic-generator"} |= "Traffic summary"' \
    --data-urlencode \
    'limit=5'
)"

printf '%s' "${LOKI_RESULT}" |
python3 -c '
import json
import sys

data = json.load(sys.stdin)
results = data["data"]["result"]

if not results:
    raise SystemExit("No traffic-generator logs found in Loki")

lines = []

for stream in results:
    for value in stream.get("values", []):
        if len(value) >= 2:
            lines.append(value[1])

if not any("Traffic summary" in line for line in lines):
    raise SystemExit("Traffic summary log not found")

print("[OK] Loki contains traffic-generator logs")
'

echo
echo "=== Grafana validation ==="

GRAFANA_PASSWORD="$(
  kubectl get secret \
    --namespace "${MONITORING_NAMESPACE}" \
    kube-prometheus-stack-grafana \
    -o jsonpath='{.data.admin-password}' |
  base64 -d
)"

DATASOURCES="$(
  curl -fsS \
    -u "admin:${GRAFANA_PASSWORD}" \
    "http://localhost:${GRAFANA_PORT}/api/datasources"
)"

printf '%s' "${DATASOURCES}" |
python3 -c '
import json
import sys

data = json.load(sys.stdin)
names = {item["name"] for item in data}

required = {"Prometheus", "Loki"}
missing = required - names

if missing:
    raise SystemExit(
        "Missing Grafana datasource(s): " + ", ".join(sorted(missing))
    )

print("[OK] Grafana has Prometheus and Loki datasources")
'

curl -fsS \
  -u "admin:${GRAFANA_PASSWORD}" \
  "http://localhost:${GRAFANA_PORT}/api/datasources/uid/loki/health" \
  >/dev/null

ok "Grafana can reach Loki datasource"

echo
echo "======================================"
echo "EnvForge observability validation PASS"
echo "======================================"
