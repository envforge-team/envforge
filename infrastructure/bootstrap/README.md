# Remote state setup

```bash
cd infrastructure/bootstrap-remote-state
chmod +x create-remote-state.sh verify-remote-state.sh
az login
az account set --subscription "<SUBSCRIPTION_ID>"

LOCATION=westeurope \
RESOURCE_GROUP=rg-envforge-tfstate \
STATE_KEY=envforge.shared.tfstate \
./create-remote-state.sh
```

Then, from the Terraform root that contains `backend.tf`:

```bash
terraform init -reconfigure \
  -backend-config="../bootstrap-remote-state/backend.hcl"
```

Verify:

```bash
cd infrastructure/bootstrap-remote-state
./verify-remote-state.sh backend.hcl
```
