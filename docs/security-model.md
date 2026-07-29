# Security Model (draft) — envforge

> Draft inițial (Săptămâna 1, Ziua 5). Se completează în Săptămânile 3 și 7 pe măsură ce se implementează efectiv autorizarea, audit-ul și integrarea Entra ID.

## 1. Identity provider

- Autentificare via **Microsoft Entra ID** (OIDC/OAuth2).
- Control API validează tokenul JWT primit de la Portal (semnătură, expirare, issuer, audience).
- Nu se stochează parole în envforge — identitatea e delegată integral către Entra ID.

## 2. Roluri

| Rol | Descriere |
|---|---|
| USER | Acces read-only: vizualizare medii, istoric deployment, metrici. |
| OPERATOR | Poate face update de versiune, rollback, extindere lifetime pe medii pe care le deține sau i s-au atribuit. |
| ADMIN | Acces complet: gestionare utilizatori/roluri, vizualizare audit complet, override pe orice environment. |

Regula generală: **least privilege** — fiecare rol are strict permisiunile necesare rolului său, nimic în plus.

## 3. Endpoint-uri protejate (draft, se detaliază în Săptămâna 3)

| Endpoint | Rol minim necesar |
|---|---|
| `GET /api/environments` | USER |
| `PATCH /api/environments/{id}` | OPERATOR |
| `DELETE /api/environments/{id}` | OPERATOR (doar owner) / ADMIN |
| `POST /api/environments/{id}/rollback` | OPERATOR (doar owner) / ADMIN |
| `GET /api/audit` | ADMIN |
| `POST /api/users/{id}/role` | ADMIN |

## 4. Ownership

- Fiecare environment are un `owner` (utilizatorul care l-a creat).
- OPERATOR poate acționa doar pe mediile proprii, în afară de cazul în care are drept explicit acordat sau e ADMIN.
- Verificarea ownership-ului se face în `AuthorizationService`, separat de verificarea rolului.

## 5. Audit

- Orice acțiune de tip create/update/delete/rollback generează un `AuditEvent`: actor, acțiune, resursă, timestamp, rezultat (succes/eșec).
- Audit log-ul este **append-only** — nu poate fi modificat sau șters prin API, nici măcar de ADMIN.
- Accesul la audit log e restricționat la rolul ADMIN.

## 6. Infrastructură / platformă

- **GitHub → Azure**: autentificare via OIDC + federated credentials, fără secrete statice (vezi `architecture-identity.md`).
- **Kubernetes**: fiecare serviciu rulează cu propriul `ServiceAccount`, permisiuni minime via `Role`/`RoleBinding`, izolare de rețea via `NetworkPolicy`.
- **Containere**: rulare non-root, imagini cu tag-uri fixe (nu `latest`), verificare imagini înainte de deploy (Săptămâna 7).
- **Secrete**: gestionate prin mecanismul de secret management ales de echipă (ex. Kubernetes Secrets / Azure Key Vault) — se detaliază în Săptămâna 7.

## 7. Ce urmează (nu face parte din Săptămâna 1)

- Implementare efectivă AuthorizationService + AuditService (Săptămâna 3).
- Integrare reală Entra ID + roluri active (Săptămâna 7).
- Network policies, security context, verificare imagini (Săptămâna 7).
- Test end-to-end de securitate: token expirat, identitate indisponibilă, permisiuni insuficiente (Săptămâna 7).
