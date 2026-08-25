#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONTEXT="${ENVFORGE_KUBE_CONTEXT:-kind-envforge}"

APP_NAMESPACE="env-reliability-demo"
MONITORING_NAMESPACE="monitoring"

INCIDENT_SECRET="reliability-demo-incident-admin"
INCIDENT_SECRET_KEY="ENVFORGE_INCIDENT_ADMIN_KEY"
INCIDENT_HEADER="X-EnvForge-Incident-Key"

PROM_SERVICE="kube-prometheus-stack-prometheus"

ALERT_NAME="EnvForgeReliabilityHigh5xxRatio"
METRIC_NAME="envforge_reliability:http_5xx_ratio:rate5m"

APP_PORT=18085
PROM_PORT=19090

APP_PF=""
PROM_PF=""
INCIDENT_KEY=""
INCIDENT_ACTIVE=false

fail() {
  echo "[FAIL] $1"
  exit 1
}

ok() {
  echo "[OK] $1"
}

cleanup() {
  echo
  echo "=== Cleanup ==="

  if [ "${INCIDENT_ACTIVE}" = "true" ] &&
     [ -n "${INCIDENT_KEY}" ] &&
     [ -n "${APP_PF}" ]; then

    curl -sS \
      -X POST \
      -H "${INCIDENT_HEADER}: ${INCIDENT_KEY}" \
      "http://localhost:${APP_PORT}/admin/incidents/reset" \
      >/dev/null 2>&1 || true

    echo "[INFO] Incident reset requested during cleanup"
  fi

  [ -n "${APP_PF}" ] &&
    kill "${APP_PF}" 2>/dev/null || true

  [ -n "${PROM_PF}" ] &&
    kill "${PROM_PF}" 2>/dev/null || true
}

trap cleanup EXIT

wait_http() {
  local url="$1"
  local name="$2"

  for _ in $(seq 1 30); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      ok "${name} is reachable"
      return 0
    fi

    sleep 1
  done

  fail "${name} did not become reachable"
}

get_alert_state() {
  curl -fsS \
    "http://localhost:${PROM_PORT}/api/v1/alerts" |
    python3 -c '
import json
import sys

name = sys.argv[1]
payload = json.load(sys.stdin)

for alert in payload.get("data", {}).get("alerts", []):
    if alert.get("labels", {}).get("alertname") == name:
        print(alert.get("state", "unknown"))
        break
' "${ALERT_NAME}"
}

get_5xx_ratio() {
  curl -fsSG \
    "http://localhost:${PROM_PORT}/api/v1/query" \
    --data-urlencode "query=${METRIC_NAME}" |
    python3 -c '
import json
import sys

payload = json.load(sys.stdin)
result = payload.get("data", {}).get("result", [])

if not result:
    print("")
    raise SystemExit(0)

print(result[0]["value"][1])
'
}

echo "=== EnvForge Day 35 Incident + Alert E2E ==="

CURRENT_CONTEXT="$(kubectl config current-context)"

if [ "${CURRENT_CONTEXT}" != "${EXPECTED_CONTEXT}" ]; then
  fail "Expected ${EXPECTED_CONTEXT}, got ${CURRENT_CONTEXT}"
fi

ok "Kubernetes context is ${CURRENT_CONTEXT}"

echo
echo "=== Pre-flight ==="

kubectl wait \
  deployment/reliability-demo-api \
  --namespace "${APP_NAMESPACE}" \
  --for=condition=available \
  --timeout=60s

kubectl wait \
  deployment/traffic-generator \
  --namespace "${APP_NAMESPACE}" \
  --for=condition=available \
  --timeout=60s

ok "Reliability workloads are available"

kubectl get service "${PROM_SERVICE}" \
  --namespace "${MONITORING_NAMESPACE}" \
  >/dev/null

ok "Prometheus Service exists"

kubectl get secret "${INCIDENT_SECRET}" \
  --namespace "${APP_NAMESPACE}" \
  >/dev/null

INCIDENT_KEY="$(
  kubectl get secret "${INCIDENT_SECRET}" \
    --namespace "${APP_NAMESPACE}" \
    -o jsonpath="{.data.${INCIDENT_SECRET_KEY}}" |
    base64 --decode
)"

if [ -z "${INCIDENT_KEY}" ]; then
  fail "Incident administration secret is empty"
fi

ok "Incident administration secret is available"

APP_POD="$(
  kubectl get pods \
    --namespace "${APP_NAMESPACE}" \
    --selector app=reliability-demo-api \
    --field-selector=status.phase=Running \
    -o jsonpath='{.items[0].metadata.name}'
)"

if [ -z "${APP_POD}" ]; then
  fail "No running reliability-demo-api pod found"
fi

echo "Selected incident target: ${APP_POD}"

echo
echo "=== Starting tunnels ==="

kubectl port-forward \
  --namespace "${APP_NAMESPACE}" \
  "pod/${APP_POD}" \
  "${APP_PORT}:8080" \
  >/tmp/envforge-day35-app-pf.log 2>&1 &

APP_PF=$!

kubectl port-forward \
  --namespace "${MONITORING_NAMESPACE}" \
  "svc/${PROM_SERVICE}" \
  "${PROM_PORT}:9090" \
  >/tmp/envforge-day35-prom-pf.log 2>&1 &

PROM_PF=$!

wait_http \
  "http://localhost:${APP_PORT}/work" \
  "Reliability Demo API"

wait_http \
  "http://localhost:${PROM_PORT}/-/ready" \
  "Prometheus"

echo
echo "=== Establishing clean baseline ==="

curl -fsS \
  -X POST \
  -H "${INCIDENT_HEADER}: ${INCIDENT_KEY}" \
  "http://localhost:${APP_PORT}/admin/incidents/reset" \
  >/dev/null

ok "Target pod incident state reset"

BASELINE_STATUS="$(
  curl -sS \
    -o /dev/null \
    -w '%{http_code}' \
    "http://localhost:${APP_PORT}/work"
)"

if [ "${BASELINE_STATUS}" != "200" ]; then
  fail "Target pod baseline returned HTTP ${BASELINE_STATUS}"
fi

ok "Target pod returns HTTP 200 before incident"

echo "Waiting for any previous ${ALERT_NAME} state to clear..."

BASELINE_CLEAR=false

for ATTEMPT in $(seq 1 72); do
  STATE="$(get_alert_state || true)"

  if [ -z "${STATE}" ]; then
    BASELINE_CLEAR=true
    break
  fi

  echo \
    "Alert still ${STATE} from previous data " \
    "(attempt ${ATTEMPT}/72); waiting 5s..."

  sleep 5
done

if [ "${BASELINE_CLEAR}" != "true" ]; then
  fail "Alert did not return to inactive baseline"
fi

ok "Alert baseline is inactive"

echo
echo "=== Triggering controlled 5xx incident ==="

TRIGGER_STATUS="$(
  curl -sS \
    -o /tmp/envforge-day35-trigger.json \
    -w '%{http_code}' \
    -X POST \
    -H "${INCIDENT_HEADER}: ${INCIDENT_KEY}" \
    "http://localhost:${APP_PORT}/admin/incidents/failure?enabled=true"
)"

if [ "${TRIGGER_STATUS}" != "200" ]; then
  fail "Incident trigger returned HTTP ${TRIGGER_STATUS}"
fi

INCIDENT_ACTIVE=true

ok "Controlled failure incident enabled"

WORK_STATUS="$(
  curl -sS \
    -o /tmp/envforge-day35-work-failure.json \
    -w '%{http_code}' \
    "http://localhost:${APP_PORT}/work"
)"

if [ "${WORK_STATUS}" != "500" ]; then
  fail "Incident target expected HTTP 500, got ${WORK_STATUS}"
fi

ok "Incident target returns HTTP 500"

echo
echo "=== Waiting for Prometheus metric ==="

METRIC_READY=false

for ATTEMPT in $(seq 1 36); do
  RATIO="$(get_5xx_ratio || true)"

  if [ -n "${RATIO}" ]; then
    if python3 - "${RATIO}" <<'PYTHON'
import sys

value = float(sys.argv[1])
raise SystemExit(0 if value > 0.05 else 1)
PYTHON
    then
      METRIC_READY=true
      break
    fi
  fi

  echo \
    "5xx ratio=${RATIO:-none}; " \
    "waiting for > 0.05 " \
    "(attempt ${ATTEMPT}/36)..."

  sleep 5
done

if [ "${METRIC_READY}" != "true" ]; then
  fail "5xx recording rule did not exceed alert threshold"
fi

echo "Observed 5xx ratio: ${RATIO}"
ok "Prometheus detects elevated 5xx ratio"

echo
echo "=== Waiting for alert ==="

ALERT_SEEN=false

for ATTEMPT in $(seq 1 36); do
  STATE="$(get_alert_state || true)"

  if [ "${STATE}" = "pending" ] ||
     [ "${STATE}" = "firing" ]; then
    ALERT_SEEN=true
    break
  fi

  echo \
    "Alert state=${STATE:-inactive}; " \
    "waiting for pending/firing " \
    "(attempt ${ATTEMPT}/36)..."

  sleep 5
done

if [ "${ALERT_SEEN}" != "true" ]; then
  fail "${ALERT_NAME} never entered pending/firing state"
fi

ok "${ALERT_NAME} entered ${STATE} state"

echo
echo "=== Waiting for FIRING state ==="

ALERT_FIRING=false

for ATTEMPT in $(seq 1 36); do
  STATE="$(get_alert_state || true)"

  if [ "${STATE}" = "firing" ]; then
    ALERT_FIRING=true
    break
  fi

  echo \
    "Alert state=${STATE:-inactive}; " \
    "waiting for firing " \
    "(attempt ${ATTEMPT}/36)..."

  sleep 5
done

if [ "${ALERT_FIRING}" != "true" ]; then
  fail "${ALERT_NAME} did not reach firing state"
fi

ok "${ALERT_NAME} is FIRING"

echo
echo "=== Recovering workload ==="

RESET_STATUS="$(
  curl -sS \
    -o /tmp/envforge-day35-reset.json \
    -w '%{http_code}' \
    -X POST \
    -H "${INCIDENT_HEADER}: ${INCIDENT_KEY}" \
    "http://localhost:${APP_PORT}/admin/incidents/reset"
)"

if [ "${RESET_STATUS}" != "200" ]; then
  fail "Incident reset returned HTTP ${RESET_STATUS}"
fi

INCIDENT_ACTIVE=false

ok "Controlled incident reset"

RECOVERY_STATUS="$(
  curl -sS \
    -o /dev/null \
    -w '%{http_code}' \
    "http://localhost:${APP_PORT}/work"
)"

if [ "${RECOVERY_STATUS}" != "200" ]; then
  fail "Target pod did not recover: HTTP ${RECOVERY_STATUS}"
fi

ok "Target pod recovered to HTTP 200"

echo
echo "=== Waiting for alert resolution ==="
echo "The recording rule uses rate5m, so resolution can take several minutes."

ALERT_RESOLVED=false

for ATTEMPT in $(seq 1 84); do
  STATE="$(get_alert_state || true)"

  if [ -z "${STATE}" ]; then
    ALERT_RESOLVED=true
    break
  fi

  RATIO="$(get_5xx_ratio || true)"

  echo \
    "Alert=${STATE}, ratio=${RATIO:-none}; " \
    "waiting for recovery " \
    "(attempt ${ATTEMPT}/84)..."

  sleep 5
done

if [ "${ALERT_RESOLVED}" != "true" ]; then
  fail "${ALERT_NAME} did not resolve after workload recovery"
fi

ok "${ALERT_NAME} resolved"

echo
echo "=== Final workload validation ==="

kubectl wait \
  deployment/reliability-demo-api \
  --namespace "${APP_NAMESPACE}" \
  --for=condition=available \
  --timeout=60s

kubectl wait \
  deployment/traffic-generator \
  --namespace "${APP_NAMESPACE}" \
  --for=condition=available \
  --timeout=60s

ok "Reliability workloads remain available"

echo
echo "=== Result ==="
echo "PASS: incident -> metric -> alert -> recovery E2E validation succeeded."
