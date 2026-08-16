#!/usr/bin/env bash
set -euo pipefail

LOCATION="${LOCATION:-westeurope}"
RESOURCE_GROUP="${RESOURCE_GROUP:-rg-envforge-tfstate}"
CONTAINER_NAME="${CONTAINER_NAME:-tfstate}"
STATE_KEY="${STATE_KEY:-envforge.shared.tfstate}"

command -v az >/dev/null 2>&1 || { echo "ERROR: Azure CLI missing."; exit 1; }
az account show >/dev/null 2>&1 || { echo "ERROR: Run az login first."; exit 1; }

SUBSCRIPTION_ID="$(az account show --query id -o tsv)"
SUFFIX="$(printf '%s' "$SUBSCRIPTION_ID" | tr -d '-' | cut -c1-8)"
STORAGE_ACCOUNT="${STORAGE_ACCOUNT:-stenvforgetf${SUFFIX}}"
STORAGE_ACCOUNT="$(printf '%s' "$STORAGE_ACCOUNT" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')"

if (( ${#STORAGE_ACCOUNT} < 3 || ${#STORAGE_ACCOUNT} > 24 )); then
  echo "ERROR: STORAGE_ACCOUNT must have 3-24 lowercase letters/numbers."
  exit 1
fi

az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none

if ! az storage account show --resource-group "$RESOURCE_GROUP" --name "$STORAGE_ACCOUNT" >/dev/null 2>&1; then
  az storage account create     --resource-group "$RESOURCE_GROUP"     --name "$STORAGE_ACCOUNT"     --location "$LOCATION"     --sku Standard_LRS     --kind StorageV2     --min-tls-version TLS1_2     --allow-blob-public-access false     --output none
fi

az storage container create   --name "$CONTAINER_NAME"   --account-name "$STORAGE_ACCOUNT"   --auth-mode login   --output none

cat > backend.hcl <<EOF
resource_group_name  = "$RESOURCE_GROUP"
storage_account_name = "$STORAGE_ACCOUNT"
container_name       = "$CONTAINER_NAME"
key                  = "$STATE_KEY"
use_azuread_auth     = true
subscription_id      = "$SUBSCRIPTION_ID"
EOF

echo "Remote state ready."
echo "Generated: $(pwd)/backend.hcl"
