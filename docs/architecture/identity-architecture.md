# Arhitectură Identity & Trust GitHub–Azure (envforge)

## 1. Flux de autentificare și autorizare (login)

```mermaid
sequenceDiagram
    actor U as Utilizator
    participant P as Portal (React)
    participant E as Microsoft Entra ID
    participant C as Control API (Spring Boot)

    U->>P: Deschide portalul
    P->>E: Redirect login (OIDC/OAuth2)
    U->>E: Introduce credențiale
    E-->>P: Returnează token (JWT)
    P->>C: Request cu Authorization: Bearer <token>
    C->>C: Validează semnătura + expirarea tokenului
    C->>C: Extrage rolul (USER/OPERATOR/ADMIN) din claims
    alt rol are permisiune
        C-->>P: 200 OK + date
    else rol fără permisiune
        C-->>P: 403 Forbidden
    end
    C->>C: Scrie eveniment de audit (actor, acțiune, timestamp)
```

Note:
- Tokenul JWT emis de Entra ID conține rolul/claims-urile necesare pentru autorizare.
- Control API nu stochează parole — validează doar tokenul primit.
- Fiecare acțiune sensibilă (update/delete/rollback) generează automat un eveniment de audit.

## 2. Trust GitHub Actions ↔ Azure (OIDC, fără secrete statice)

```mermaid
sequenceDiagram
    participant GH as GitHub Actions (pipeline)
    participant GHOIDC as GitHub OIDC Provider
    participant AAD as Azure AD (Federated Credential)
    participant AZ as Azure (ACR / AKS)

    GH->>GHOIDC: Cere token OIDC pentru acest workflow run
    GHOIDC-->>GH: Emite token JWT semnat (scurtă durată)
    GH->>AAD: azure/login cu tokenul OIDC
    AAD->>AAD: Verifică federated credential (issuer, repo, branch, subject)
    AAD-->>GH: Emite token de acces Azure (scurtă durată)
    GH->>AZ: Operații (push imagine în ACR, deploy pe AKS) cu tokenul Azure
```

Note:
- Nu există client secret stocat în GitHub Secrets — trust-ul se bazează pe federated credential configurat în Azure AD, care are încredere doar în workflow-uri din repo-ul/branch-ul specificat.
- Fiecare token e temporar — reduce riscul în caz de scurgere de credențiale.
- Configurația se face în Terraform, modulul `identities` (Săptămâna 5), și se verifică accesul OIDC în Săptămâna 6.

## 3. Roluri și ce pot face

| Rol | Vizualizare medii | Update / Rollback | Gestionare useri/roluri | Vizualizare audit |
|---|---|---|---|---|
| USER | Da | Nu | Nu | Nu |
| OPERATOR | Da | Da | Nu | Nu |
| ADMIN | Da | Da | Da | Da |

## 4. Componente implicate

- **Portal (React)** — pagini Login, Profile, Audit.
- **Control API (Spring Boot)** — validare token, AuthorizationService, AuditService.
- **Microsoft Entra ID** — identity provider, emite tokenuri JWT.
- **Azure AD Federated Credentials** — trust OIDC pentru pipeline-uri GitHub Actions.
- **AKS / ACR** — resurse Azure accesate securizat de pipeline-uri, fără secrete statice.
