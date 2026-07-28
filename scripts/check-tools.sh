#!/usr/bin/env bash
set -u

checks=(
  "Git|git|git --version"
  "Java|java|java -version"
  "Maven|mvn|mvn -version"
  "Node.js|node|node --version"
  "npm|npm|npm --version"
  "Docker|docker|docker --version"
  "kubectl|kubectl|kubectl version --client"
  "Helm|helm|helm version --short"
  "Terraform|terraform|terraform version"
  "Azure CLI|az|az version"
)

failed=0
for row in "${checks[@]}"; do
  IFS='|' read -r name exe command <<< "$row"
  if command -v "$exe" >/dev/null 2>&1; then
    echo "[OK] $name"
    bash -lc "$command" 2>&1 | head -n 3
  else
    echo "[MISSING] $name"
    failed=1
  fi
  echo
done

if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    echo "[OK] Docker engine is running"
  else
    echo "[WARNING] Docker is installed but its engine is unavailable"
  fi
fi

exit "$failed"
