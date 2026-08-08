#!/usr/bin/env bash
set -euo pipefail

BACKEND_FILE="${1:-backend.hcl}"
[[ -f "$BACKEND_FILE" ]] || { echo "ERROR: $BACKEND_FILE missing."; exit 1; }

read_value() {
  sed -nE "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*\"([^\"]+)\".*/\1/p" "$BACKEND_FILE" | head -n1
}

RESOURCE_GROUP="$(read_value resource_group_name)"
STORAGE_ACCOUNT="$(read_value storage_account_name)"
CONTAINER_NAME="$(read_value container_name)"
STATE_KEY="$(read_value key)"

az storage account show   --resource-group "$RESOURCE_GROUP"   --name "$STORAGE_ACCOUNT"   --output table

az storage container show   --account-name "$STORAGE_ACCOUNT"   --name "$CONTAINER_NAME"   --auth-mode login   --output table

az storage blob list   --account-name "$STORAGE_ACCOUNT"   --container-name "$CONTAINER_NAME"   --auth-mode login   --query '[].{name:name,lastModified:properties.lastModified}'   --output table

echo "Expected key: $STATE_KEY"
