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

PROM_TARGETS_READY=""

for ATTEMPT in $(seq 1 18); do
  PROM_UP="$(
    curl -GsS \
      "http://localhost:${PROM_PORT}/api/v1/query" \
      --data-urlencode \
      'query=up{service="reliability-demo-api-metrics"}'
  )"

  PROM_TARGETS_READY="$(
    printf '%s' "${PROM_UP}" |
    python3 -c '
import json
import sys

data = json.load(sys.stdin)
results = data["data"]["result"]

if len(results) < 2:
    print("")
    raise SystemExit(0)

values = [float(item["value"][1]) for item in results]

if not all(value == 1.0 for value in values):
    print("")
    raise SystemExit(0)

print(len(results))
'
  )"

  if [ -n "${PROM_TARGETS_READY}" ]; then
    break
  fi

  echo     "Prometheus targets not ready yet "     "(attempt ${ATTEMPT}/18); waiting 5s..."

  sleep 5
done

if [ -z "${PROM_TARGETS_READY}" ]; then
  fail "Expected at least 2 Prometheus targets UP after 90 seconds"
fi

ok "Prometheus reports ${PROM_TARGETS_READY} workload targets UP"

HTTP_REQUEST_COUNT=""

for ATTEMPT in $(seq 1 12); do
  HTTP_METRICS="$(
    curl -GsS \
      "http://localhost:${PROM_PORT}/api/v1/query" \
      --data-urlencode \
      'query=sum(http_server_requests_seconds_count{namespace="env-reliability-demo",uri="/work"})'
  )"

  HTTP_REQUEST_COUNT="$(
    printf '%s' "${HTTP_METRICS}" |
    python3 -c '
import json
import sys

data = json.load(sys.stdin)
results = data["data"]["result"]

if not results:
    print("")
    raise SystemExit(0)

value = float(results[0]["value"][1])

if value <= 0:
    print("")
    raise SystemExit(0)

print(value)
'
  )"

  if [ -n "${HTTP_REQUEST_COUNT}" ]; then
    break
  fi

  echo     "HTTP workload metrics not ready yet "     "(attempt ${ATTEMPT}/12); waiting 5s..."

  sleep 5
done

if [ -z "${HTTP_REQUEST_COUNT}" ]; then
  fail "No /work request metric available after 60 seconds"
fi

python3 - "${HTTP_REQUEST_COUNT}" <<'PYTHON'
import sys

value = float(sys.argv[1])
print(f"[OK] Prometheus /work request count = {value:g}")
PYTHON

echo
echo "=== Prometheus recording rules ==="

RECORDED_TARGETS_VALUE=""

for ATTEMPT in $(seq 1 18); do
  RECORDED_TARGETS="$(
    curl -GsS \
      "http://localhost:${PROM_PORT}/api/v1/query" \
      --data-urlencode \
      'query=envforge_reliability:targets_up'
  )"

  RECORDED_TARGETS_VALUE="$(
    printf '%s' "${RECORDED_TARGETS}" |
    python3 -c '
import json
import sys

data = json.load(sys.stdin)
results = data["data"]["result"]

if results:
    print(results[0]["value"][1])
'
  )"

  if [ -n "${RECORDED_TARGETS_VALUE}" ]; then
    if python3 - "${RECORDED_TARGETS_VALUE}" <<'PYTHON'
import sys

value = float(sys.argv[1])
raise SystemExit(0 if value >= 2 else 1)
PYTHON
    then
      break
    fi
  fi

  echo     "Recording rule not ready yet: "     "value=${RECORDED_TARGETS_VALUE:-none} "     "(attempt ${ATTEMPT}/18); waiting 5s..."

  sleep 5
done

if [ -z "${RECORDED_TARGETS_VALUE}" ]; then
  fail "Recording rule targets_up returned no result after 90 seconds"
fi

python3 - "${RECORDED_TARGETS_VALUE}" <<'PYTHON'
import sys

value = float(sys.argv[1])

if value < 2:
    raise SystemExit(
        f"Expected at least 2 recorded targets UP after retries, got {value:g}"
    )

print(f"[OK] Recording rule reports {value:g} workload targets UP")
PYTHON

echo
echo "=== Prometheus reliability alerts ==="

ALERTS_READY=""

for ATTEMPT in $(seq 1 18); do
  ALERT_RULES="$(
    curl -fsS       "http://localhost:${PROM_PORT}/api/v1/rules?type=alert"
  )"

  ALERTS_READY="$(
    printf '%s' "${ALERT_RULES}" |
    python3 -c '
import json
import sys

wanted = {
    "EnvForgeReliabilityTargetDown",
    "EnvForgeReliabilityHigh5xxRatio",
    "EnvForgeReliabilityHighLatency",
    "EnvForgeReliabilityHighCpu",
}

data = json.load(sys.stdin)

found = set()

for group in data["data"]["groups"]:
    for rule in group.get("rules", []):
        name = rule.get("name")
        if name in wanted:
            found.add(name)

if found == wanted:
    print("ready")
'
  )"

  if [ "${ALERTS_READY}" = "ready" ]; then
    break
  fi

  echo     "Reliability alert rules not ready yet "     "(attempt ${ATTEMPT}/18); waiting 5s..."

  sleep 5
done

if [ "${ALERTS_READY}" != "ready" ]; then
  fail "Reliability alert rules were not loaded after 90 seconds"
fi

ok "Prometheus loaded all 4 reliability alert rules"

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

DASHBOARD="$(
  curl -fsS \
    -u "admin:${GRAFANA_PASSWORD}" \
    "http://localhost:${GRAFANA_PORT}/api/dashboards/uid/envforge-reliability"
)"

printf '%s' "${DASHBOARD}" |
python3 -c '
import json
import sys

data = json.load(sys.stdin)
dashboard = data["dashboard"]

if dashboard.get("uid") != "envforge-reliability":
    raise SystemExit("Reliability dashboard UID is invalid")

panels = dashboard.get("panels", [])

if len(panels) < 10:
    raise SystemExit(
        f"Expected at least 10 dashboard panels, got {len(panels)}"
    )

print(
    f"[OK] Grafana reliability dashboard loaded "
    f"with {len(panels)} panels"
)
'

echo
echo "======================================"
echo "EnvForge observability validation PASS"
echo "======================================"
