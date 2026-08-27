# Security Model — envforge

> Draft inițial: Săptămâna 1, Ziua 5. Finalizat: Săptămâna 8, Ziua 38,
> pe baza implementării efective (Săptămânile 3, 7 și 8). Reflectă
> starea reală a codului la data acestui commit, nu planul inițial.

## 1. Identity provider

- Autentificare via **Microsoft Entra ID** (OIDC/OAuth2), profil Spring
  `entra` (`EntraSecurityConfig` + `spring-boot-starter-oauth2-resource-server`).
- Control API validează tokenul JWT (semnătură, expirare, issuer) prin
  `NimbusJwtDecoder`, construit automat de Spring Boot din
  `issuer-uri`.
- Un profil separat `!entra` (implicit, `DevCurrentUserProvider`) oferă
  o identitate locală permisivă pentru dezvoltare și pentru suita de
  teste existentă — cele două profiluri sunt izolate prin `@Profile`
  și nu se ating reciproc.
- Rolul folosit pentru autorizare vine din baza de date
  (`UserEntity.role`), nu direct din claim-urile JWT — JWT-ul stabilește
  doar *cine* e utilizatorul (`sub`, `preferred_username`/`email`,
  `name`); rolul e citit/creat de `EntraIdCurrentUserProvider` din
  `UserRepository`, cu fallback pe un email de bootstrap-admin
  configurat pentru identități noi.
- Nu se stochează parole în envforge — identitatea e delegată integral
  către Entra ID.

## 2. Roluri

| Rol | Descriere |
|---|---|
| USER | Acces read-only de bază; poate vedea propriul profil (`/api/me`). |
| OPERATOR | Poate vizualiza date de monitoring pentru medii. |
| ADMIN | Gestionare roluri utilizatori, vizualizare audit complet. |

Regula generală: **least privilege** — fiecare rol are strict
permisiunile necesare rolului său, nimic în plus. Enforcement-ul se
face central, în `AuthorizationService.requireRole` /
`requireAdmin` / `requireOwnerOrAdmin`, pe baza `CurrentUser.role()` —
independent de mecanismul de autentificare (dev sau Entra).

## 3. Endpoint-uri protejate (stare reală, control-api)

| Endpoint | Rol minim necesar | Enforced în |
|---|---|---|
| `GET /api/me` | orice utilizator autentificat | — (nu necesită rol specific) |
| `PUT /api/users/{id}/role` | ADMIN | `UserController` → `requireAdmin` |
| `GET /api/audit` | ADMIN | `AuditController` → `requireAdmin` |
| `GET /api/environments/{id}/monitoring/metrics` | OPERATOR / ADMIN | `MonitoringController` → `requireRole` |
| `GET /api/environments/{id}/monitoring/events` | OPERATOR / ADMIN | `MonitoringController` → `requireRole` |

**Gap cunoscut, flagged:** `EnvironmentController` (`POST /api/environments`,
`GET /api/environments`, `GET /api/environments/{id}`) nu are încă
niciun control de rol sau ownership — orice utilizator autentificat
poate crea și vedea orice environment. Modulul de environments nu e în
responsabilitatea Security/Identity/Platform; semnalat echipei, nu
modificat unilateral aici. `requireOwnerOrAdmin` există deja în
`AuthorizationService`, pregătit pentru momentul în care
`EnvironmentController` va adopta ownership-ul pe resurse.

`reliability-demo-api` (`IncidentController`) folosește un mecanism
separat, intenționat simplu — o cheie partajată trimisă în header-ul
`X-EnvForge-Incident-Key`, comparată în timp constant. Nu face parte
din sistemul de roluri USER/OPERATOR/ADMIN; e o aplicație demo pentru
testarea rezilienței, nu un serviciu de producție.

## 4. Ownership

- `AuthorizationService.requireOwnerOrAdmin` există și e testat
  (permite ADMIN necondiționat; pentru non-ADMIN, cere atât
  potrivirea de owner cât și rol minim OPERATOR).
- Nu are încă niciun apelant real în cod — pregătit pentru momentul în
  care un modul (ex. environments) va introduce acțiuni pe resurse
  proprii.

## 5. Audit

- Orice refuz de autorizare (`AuthorizationService`) generează automat
  un `AuditEvent` de tip FAILURE, înainte de a arunca excepția.
- `UserController.updateRole` generează un `AuditEvent` de succes la
  schimbarea unui rol.
- Audit log-ul este **append-only** — nu poate fi modificat sau șters
  prin API.
- Accesul la audit log (`GET /api/audit`) e restricționat la ADMIN.

## 6. Metrici și alerting

- `SecurityMetrics` (Micrometer) expune contoare pentru login, 401 și
  403 (Ziua 29).
- Dashboard Grafana (`observability/dashboards/security-overview.json`)
  și alertă Prometheus pe acces refuzat excesiv (Ziua 30).

## 7. Infrastructură / platformă

- **GitHub → Azure**: autentificare OIDC + federated credential, fără
  secrete statice — infrastructura există în Terraform (modulul
  `identities`), dar niciun workflow GitHub Actions nu o folosește
  încă (vezi `docs/architecture/day38-oidc-setup-guide.md` și
  `docs/observability/day26-oidc-status.md`). Echipa lucrează pe Kind
  local; AKS e blocat de quota de subscripție Azure.
- **Kubernetes**: fiecare serviciu rulează cu propriul
  `ServiceAccount`, permisiuni minime via `Role`/`RoleBinding`
  (read/list/watch), izolare de rețea via `NetworkPolicy` (Ziua 17,
  reverificat Ziua 33).
- **Containere**: `runAsNonRoot`, `seccompProfile: RuntimeDefault`,
  `allowPrivilegeEscalation: false`, toate capabilities eliminate
  (Ziua 33). control-api rulează explicit cu `runAsUser: 100`
  (imaginea are un `USER` numit, nu UID numeric).
- **Secrete**: parola bazei de date e livrată prin `Secret`
  Kubernetes (`control-api-db`, via `secretKeyRef`), nu ca variabilă
  de mediu simplă (Ziua 33). Valoarea sursă rămâne deocamdată în
  `values.yaml`; migrarea către Azure Key Vault rămâne un TODO.

## 8. Testare negativă

`EntraNegativePathsTest` (Ziua 34) verifică: token expirat respins de
`JwtValidators.createDefault()`, JWT fără claim-uri de identitate
(fallback la `sub`), rol insuficient → tot refuzat, indiferent de
mecanismul de autentificare.

## 9. Ce rămâne deschis

- Test happy-path cu token Entra real (`az login` device-code) —
  blocat pe consimțământ interactiv, amânat (Ziua 31).
- Creare AKS — blocată de moratoriul echipei pe `terraform apply`
  (quota subscripție), Ziua 25.
- Identitate Azure netrackuită (`id-github-actions-envforge-dev`) —
  neimportată în state-ul Terraform.
- Drift `module.network` cu modificările lui M1 — nerezolvat.
- Ownership/rol pe `EnvironmentController` — gap cunoscut, semnalat
  echipei (secțiunea 3).
- Migrare parolă DB din `values.yaml` către Azure Key Vault.
