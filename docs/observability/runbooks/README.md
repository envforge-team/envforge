# EnvForge Reliability Runbooks

Operational runbooks for the EnvForge reliability and monitoring stack.

| Incident | Runbook |
| --- | --- |
| Elevated HTTP 5xx rate | [http-5xx.md](http-5xx.md) |
| High application CPU | [high-cpu.md](high-cpu.md) |
| Unexpected pod restart | [pod-restart.md](pod-restart.md) |
| Monitoring unavailable | [monitoring-unavailable.md](monitoring-unavailable.md) |

These procedures target the local Kind-based EnvForge environment.

After incident recovery, run:

```bash
ENVFORGE_KUBE_CONTEXT="$(kubectl config current-context)" \
./observability/scripts/validate-kind-observability.sh
```

A successful recovery should end with:

`EnvForge observability validation PASS`
