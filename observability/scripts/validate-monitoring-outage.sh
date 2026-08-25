#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONTEXT="${ENVFORGE_KUBE_CONTEXT:-kind-envforge}"
APP_NAMESPACE="env-reliability-demo"
MONITORING_NAMESPACE="monitoring"

PROM_STS="prometheus-kube-prometheus-stack-prometheus"
GRAFANA_DEPLOYMENT="kube-prometheus-stack-grafana"
PROM_OPERATOR_DEPLOYMENT="kube-prometheus-stack-operator"

APP_PORT=18084
APP_PF=""

PROM_REPLICAS=""
GRAFANA_REPLICAS=""
OPERATOR_REPLICAS=""
OUTAGE_STARTED=false

fail() {
  echo "[FAIL] $1"
  exit 1
}

ok() {
  echo "[OK] $1"
}

cleanup() {
  echo
  echo "=== Restoring monitoring stack ==="

  if [ -n "${APP_PF}" ]; then
    kill "${APP_PF}" 2>/dev/null || true
  fi

  if [ "${OUTAGE_STARTED}" = "true" ]; then
    if [ -n "${PROM_REPLICAS}" ]; then
      kubectl scale \
        statefulset "${PROM_STS}" \
        --namespace "${MONITORING_NAMESPACE}" \
        --replicas="${PROM_REPLICAS}" \
        >/dev/null 2>&1 || true
    fi

    if [ -n "${GRAFANA_REPLICAS}" ]; then
      kubectl scale \
        deployment "${GRAFANA_DEPLOYMENT}" \
        --namespace "${MONITORING_NAMESPACE}" \
        --replicas="${GRAFANA_REPLICAS}" \
        >/dev/null 2>&1 || true
    fi

    if [ -n "${OPERATOR_REPLICAS}" ]; then
      kubectl scale \
        deployment "${PROM_OPERATOR_DEPLOYMENT}" \
        --namespace "${MONITORING_NAMESPACE}" \
        --replicas="${OPERATOR_REPLICAS}" \
        >/dev/null 2>&1 || true
    fi
  fi
}

trap cleanup EXIT

echo "=== EnvForge Monitoring Outage Validation ==="

CURRENT_CONTEXT="$(kubectl config current-context)"

if [ "${CURRENT_CONTEXT}" != "${EXPECTED_CONTEXT}" ]; then
  fail "Expected context ${EXPECTED_CONTEXT}, got ${CURRENT_CONTEXT}"
fi

ok "Kubernetes context is ${CURRENT_CONTEXT}"

echo
echo "=== Pre-flight validation ==="

kubectl get statefulset "${PROM_STS}" \
  --namespace "${MONITORING_NAMESPACE}" \
  >/dev/null

kubectl get deployment "${GRAFANA_DEPLOYMENT}" \
  --namespace "${MONITORING_NAMESPACE}" \
  >/dev/null

kubectl get deployment "${PROM_OPERATOR_DEPLOYMENT}" \
  --namespace "${MONITORING_NAMESPACE}" \
  >/dev/null

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

ok "Reliability workloads are available before outage"

PROM_REPLICAS="$(
  kubectl get statefulset "${PROM_STS}" \
    --namespace "${MONITORING_NAMESPACE}" \
    -o jsonpath='{.spec.replicas}'
)"

GRAFANA_REPLICAS="$(
  kubectl get deployment "${GRAFANA_DEPLOYMENT}" \
    --namespace "${MONITORING_NAMESPACE}" \
    -o jsonpath='{.spec.replicas}'
)"

OPERATOR_REPLICAS="$(
  kubectl get deployment "${PROM_OPERATOR_DEPLOYMENT}" \
    --namespace "${MONITORING_NAMESPACE}" \
    -o jsonpath='{.spec.replicas}'
)"

echo "Prometheus replicas before test: ${PROM_REPLICAS}"
echo "Grafana replicas before test: ${GRAFANA_REPLICAS}"
echo "Prometheus Operator replicas before test: ${OPERATOR_REPLICAS}"

echo
echo "=== Starting application tunnel ==="

kubectl \
  --namespace "${APP_NAMESPACE}" \
  port-forward \
  svc/reliability-demo-api \
  "${APP_PORT}:80" \
  >/tmp/envforge-day34-app-port-forward.log 2>&1 &

APP_PF=$!

for _ in $(seq 1 30); do
  if curl -fsS \
    "http://localhost:${APP_PORT}/work" \
    >/dev/null 2>&1; then
    break
  fi

  sleep 1
done

if ! curl -fsS \
  "http://localhost:${APP_PORT}/work" \
  >/dev/null; then
  fail "Reliability workload is not reachable before outage"
fi

ok "Application endpoint works before monitoring outage"

echo
echo "=== Simulating monitoring outage ==="

# Stop the operator first so it does not reconcile Prometheus back up.
kubectl scale \
  deployment "${PROM_OPERATOR_DEPLOYMENT}" \
  --namespace "${MONITORING_NAMESPACE}" \
  --replicas=0

kubectl rollout status \
  deployment "${PROM_OPERATOR_DEPLOYMENT}" \
  --namespace "${MONITORING_NAMESPACE}" \
  --timeout=60s

kubectl scale \
  statefulset "${PROM_STS}" \
  --namespace "${MONITORING_NAMESPACE}" \
  --replicas=0

kubectl scale \
  deployment "${GRAFANA_DEPLOYMENT}" \
  --namespace "${MONITORING_NAMESPACE}" \
  --replicas=0

OUTAGE_STARTED=true

for _ in $(seq 1 30); do
  PROM_READY="$(
    kubectl get statefulset "${PROM_STS}" \
      --namespace "${MONITORING_NAMESPACE}" \
      -o jsonpath='{.status.readyReplicas}'
  )"

  GRAFANA_READY="$(
    kubectl get deployment "${GRAFANA_DEPLOYMENT}" \
      --namespace "${MONITORING_NAMESPACE}" \
      -o jsonpath='{.status.readyReplicas}'
  )"

  PROM_READY="${PROM_READY:-0}"
  GRAFANA_READY="${GRAFANA_READY:-0}"

  if [ "${PROM_READY}" = "0" ] &&
     [ "${GRAFANA_READY}" = "0" ]; then
    break
  fi

  sleep 2
done

PROM_READY="$(
  kubectl get statefulset "${PROM_STS}" \
    --namespace "${MONITORING_NAMESPACE}" \
    -o jsonpath='{.status.readyReplicas}'
)"
PROM_READY="${PROM_READY:-0}"

GRAFANA_READY="$(
  kubectl get deployment "${GRAFANA_DEPLOYMENT}" \
    --namespace "${MONITORING_NAMESPACE}" \
    -o jsonpath='{.status.readyReplicas}'
)"
GRAFANA_READY="${GRAFANA_READY:-0}"

if [ "${PROM_READY}" != "0" ]; then
  fail "Prometheus is still available during outage simulation"
fi

if [ "${GRAFANA_READY}" != "0" ]; then
  fail "Grafana is still available during outage simulation"
fi

ok "Prometheus is unavailable as expected"
ok "Grafana is unavailable as expected"

echo
echo "=== Platform availability during outage ==="

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

ok "Reliability Demo API remains available"
ok "Traffic generator remains available"

HTTP_STATUS="$(
  curl -sS \
    -o /tmp/envforge-day34-work-response.json \
    -w '%{http_code}' \
    "http://localhost:${APP_PORT}/work"
)"

if [ "${HTTP_STATUS}" != "200" ]; then
  fail "Application returned HTTP ${HTTP_STATUS} while monitoring was unavailable"
fi

ok "Application endpoint returns HTTP 200 during monitoring outage"

sleep 5

TRAFFIC_LOGS="$(
  kubectl logs \
    --namespace "${APP_NAMESPACE}" \
    deployment/traffic-generator \
    --since=30s \
    2>/dev/null || true
)"

if [ -z "${TRAFFIC_LOGS}" ]; then
  fail "Traffic generator produced no logs during monitoring outage"
fi

ok "Traffic generator continues operating during monitoring outage"

echo
echo "=== Restoring monitoring ==="

kubectl scale \
  statefulset "${PROM_STS}" \
  --namespace "${MONITORING_NAMESPACE}" \
  --replicas="${PROM_REPLICAS}"

kubectl scale \
  deployment "${GRAFANA_DEPLOYMENT}" \
  --namespace "${MONITORING_NAMESPACE}" \
  --replicas="${GRAFANA_REPLICAS}"

kubectl scale \
  deployment "${PROM_OPERATOR_DEPLOYMENT}" \
  --namespace "${MONITORING_NAMESPACE}" \
  --replicas="${OPERATOR_REPLICAS}"

OUTAGE_STARTED=false

if [ "${OPERATOR_REPLICAS}" -gt 0 ]; then
  kubectl rollout status \
    deployment "${PROM_OPERATOR_DEPLOYMENT}" \
    --namespace "${MONITORING_NAMESPACE}" \
    --timeout=180s
fi

if [ "${GRAFANA_REPLICAS}" -gt 0 ]; then
  kubectl rollout status \
    deployment "${GRAFANA_DEPLOYMENT}" \
    --namespace "${MONITORING_NAMESPACE}" \
    --timeout=180s
fi

if [ "${PROM_REPLICAS}" -gt 0 ]; then
  kubectl rollout status \
    statefulset "${PROM_STS}" \
    --namespace "${MONITORING_NAMESPACE}" \
    --timeout=180s
fi

ok "Monitoring stack restored"

echo
echo "=== Result ==="
echo "PASS: Prometheus/Grafana outage does not block the reliability workload."
