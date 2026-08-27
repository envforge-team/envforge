# Day 38 - GitHub OIDC → Azure Setup Guide

## Goal

Document how to actually wire a GitHub Actions workflow to Azure using
the OIDC federated credential that already exists in Terraform
(`infrastructure/modules/identities`), so the next person who needs
`azure/login` in a workflow doesn't have to reverse-engineer it.

This complements `docs/observability/day26-oidc-status.md`, which
confirmed the IaC foundation exists but is not live-validated because
no workflow currently uses it (the team works against local Kind, not
AKS). This doc is the "how to actually use it" counterpart to that
status report.

## What already exists

`infrastructure/modules/identities/main.tf` creates:

- a user-assigned managed identity, `azurerm_user_assigned_identity.github_actions`
  (name from `var.github_actions_identity_name`, currently
  `id-github-actions-envforge-dev` in `dev.tfvars`)
- a federated identity credential on that identity:
  - issuer: `https://token.actions.githubusercontent.com`
  - audience: `api://AzureADTokenExchange`
  - subject: `repo:${github_repository}:ref:${github_federated_ref}`,
    currently `repo:envforge-team/envforge:ref:refs/heads/main`
- a `Contributor` role assignment for that identity (reviewed Day 24,
  judged justified for this stage)

The module exports `github_actions_identity_client_id`, explicitly
labelled for use as `AZURE_CLIENT_ID` in a workflow.

**Note on the subject claim:** it's pinned to `ref:refs/heads/main`,
which only matches token exchanges from workflow runs triggered
directly on `main` (`push` events). A `pull_request` workflow run
presents a different subject (`pull_request` ref, not
`refs/heads/main`) and **will not** match this federated credential as
configured. If a workflow needs OIDC on pull requests too, an
additional federated credential (or a broader subject pattern) is
needed — out of scope for now since no workflow uses this yet.

## What's still missing

No workflow file currently sets `permissions: id-token: write` or
calls `azure/login`. This is expected — Terraform apply is halted
team-wide (subscription quota), so there's nothing in Azure worth
authenticating to from CI right now. When that unblocks, here's the
shape of the step to add:

```yaml
permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: azure/login@v2
        with:
          client-id: ${{ vars.AZURE_CLIENT_ID }}
          tenant-id: ${{ vars.AZURE_TENANT_ID }}
          subscription-id: ${{ vars.AZURE_SUBSCRIPTION_ID }}
```

`client-id` should be the Terraform output
`github_actions_identity_client_id` (set as a repository/environment
variable, not a secret — it's not sensitive on its own without the
federated trust relationship). No `client-secret` is used or needed;
that's the entire point of OIDC federation.

## Possible connection to the Ziua 24 unexplained identity

During Ziua 24's Terraform review, an untracked Azure identity named
`id-github-actions-envforge-dev` was found in Azure but not in
Terraform state (not yet imported, left unresolved — see
`docs/architecture/azure-identity-model.md`). That name is *exactly*
`var.github_actions_identity_name` from `dev.tfvars`. This is very
likely the same resource this module is meant to create — possibly
created by an early partial `apply` before state was fully
reconciled — rather than an unrelated/suspicious resource. Worth
checking first (`terraform state show` / compare resource IDs) before
assuming anything else when that item is picked back up; not
confirmed here, just flagged as a strong lead.

## Verifying it works (once a workflow uses it)

```bash
gh run view <run-id> --log | grep -i "azure/login"
```

A successful `azure/login` step logs the resolved subscription and
tenant without ever printing a secret. If it fails with an
`AADSTS70021` or similar federated-credential error, the most common
causes are: wrong subject (see the pull_request caveat above), wrong
audience, or the identity/federated-credential Terraform resources not
actually applied yet.
