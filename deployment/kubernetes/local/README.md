# EnvForge local Kubernetes cluster

This directory contains the local Kubernetes configuration used when an
Azure AKS cluster is unavailable.

## Create the cluster

```bash
kind create cluster \
  --name envforge \
  --config deployment/kubernetes/local/kind-config.yaml \
  --wait 120s