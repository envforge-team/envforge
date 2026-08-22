# Azure identity model (AKS module)

## Scope

This document defines the identity and permission model for the `aks` and
`identities` Terraform modules (owned by M5), used to provision the AKS
cluster and its managed identities in `rg-reliability-platform-dev`. It
complements `docs/architecture/azure-governance.md`, which covers naming,
tagging and overall Terraform module ownership.

## Managed identities

| Identity | Type | Used for |
| --- | --- | --- |
| AKS control plane identity | User-assigned | Cluster control plane operations (manage NICs, load balancers in the AKS subnet) |
| AKS kubelet identity | User-assigned | Pulling container images from ACR on behalf of the nodes |
| control-api workload identity | User-assigned (federated, later) | Pod-level access to Azure resources via Workload Identity, once integrated in Saptamana 7 |

User-assigned identities are used instead of system-assigned ones so that
role assignments can be created in Terraform before the AKS resource
itself exists, and so the identity lifecycle is independent from the
cluster lifecycle.

## Role assignments

| Principal | Role | Scope | Reason |
| --- | --- | --- | --- |
| AKS control plane identity | Network Contributor | AKS subnet (`snet-aks`, from M1's network module) | Manage NICs and load balancers inside the AKS subnet |
| AKS kubelet identity | AcrPull | ACR (M2's module) | Pull container images without image pull secrets |
| GitHub Actions OIDC identity | Contributor | `rg-reliability-platform-dev` | Deploy and update infrastructure from CI (Saptamana 6, Ziua 26) |

## Federated credentials (GitHub OIDC)

Planned for Ziua 23, first used in Ziua 26:

```text
Issuer:   https://token.actions.githubusercontent.com
Subject:  repo:envforge-team/envforge:ref:refs/heads/main
Audience: api://AzureADTokenExchange
```

Scoping the subject to `refs/heads/main` means only workflow runs on the
`main` branch can exchange a token for Azure access. This avoids storing
long-lived Azure client secrets in GitHub, per the safety rules in
`docs/architecture/azure-governance.md`.

## Team access (current state, verified)

All four team members currently hold, on `rg-reliability-platform-dev`:

- `Contributor`
- `Role Based Access Control Administrator`

This is broader than least privilege for a production environment, but is
appropriate here: the group is small (4 people), the environment is a
shared dev/learning environment rather than production, and RBAC
Administrator lets each member grant the role assignments their own
Terraform module needs (for example ACR pull, AKS Network Contributor)
without depending on a single administrator for every change. Ziua 24
revisits this to catch and remove any assignment wider than what a module
actually needs.

## Open items for Ziua 22-23

- Confirm the ACR resource name/ID once M2's module is applied (needed for
  the AcrPull scope).
- The AKS subnet ID is already available as a module output
  (`module.network.aks_subnet_id`) and will be reused directly.
- Coordinate with M1/M4 before applying `identities` module role
  assignments that reference resources owned by their modules.

## Ziua 24 - verificare plan si permisiuni (fara apply)

Verificare facuta doar cu `terraform plan`, fara `terraform apply` - echipa a
decis sa opreasca apply-ul temporar (subscription quota blocata pentru AKS).
Clusterul local de dezvoltare a trecut pe Kind.

### Permisiuni module aks/identities - verificate, deja minime
- `aks_control_plane_network_contributor`: scope = doar subnet-ul AKS, nu tot VNet-ul
- `aks_control_plane_managed_identity_operator`: scope = doar identitatea kubelet
- `aks_kubelet_acr_pull`: scope = doar ACR-ul (modulul lui M2)
- `github_actions_contributor`: scope = tot resource group-ul - justificat, e
  identitatea CI comuna pentru intreg pipeline-ul de infra al echipei, nu doar
  pentru modulele mele; scoparea la nivel de RG (nu subscriptie) e limita corecta

Nu au fost gasite permisiuni de redus in modulele proprii.

### Permisiuni echipa (toti cei 4 membri) - neschimbate
Toti cei 4 membri au inca `Contributor` + `Role Based Access Control
Administrator` pe tot RG-ul, la fel ca la Ziua 21. Ramane decizia justificata
atunci: echipa mica, mediu de dev/learning, fiecare are nevoie de RBAC
Administrator ca sa-si aplice propriile role assignment-uri din Terraform.

### Descoperire: identitate neurmarita in state
Exista deja in Azure o identitate `id-github-actions-envforge-dev` cu rol
Contributor pe RG, cu exact numele definit in modulul `identities`, dar
`terraform plan` o arata ca `+ create` - nu e in state-ul remote comun.
Activity log (ultimele 30 zile) nu arata cine a creat-o. Nu a fost inca
importata in state - de facut inainte de orice `terraform apply` viitor,
altfel Terraform va incerca sa creeze un duplicat.

### Blocaj neschimbat: state modul network
`module.network` (vnet + 2 subnet-uri) tot apare ca `+ create` desi exista in
Azure - discutat la Ziua 21-24, ramane blocat pe coordonare cu M1.

### Concluzie Ziua 24
Ziua 25 (creare efectiva AKS) ramane in asteptare pana se rezolva subscription
quota la nivel de echipa si pana se reconciliaza state-ul (identitate GitHub
Actions + modul network) inainte de orice apply viitor.
