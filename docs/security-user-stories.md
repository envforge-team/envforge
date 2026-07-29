# User Stories — Security, Identity & Platform (envforge)

## Autentificare (Login)

**US-01 — Login cu contul organizației**
Ca utilizator, vreau să mă autentific folosind contul organizației (Microsoft Entra ID), ca să nu am nevoie de un cont separat pentru envforge.
Criterii de acceptare:
- Given nu sunt autentificat, When accesez portalul, Then sunt redirecționat către pagina de login Entra ID.
- Given autentificarea reușește, When mă întorc în portal, Then primesc un token valid și văd dashboard-ul.
- Given autentificarea eșuează, When mă întorc în portal, Then văd un mesaj de eroare clar, fără detalii tehnice sensibile.

**US-02 — Sesiune expirată**
Ca utilizator, vreau să fiu anunțat clar când sesiunea mea a expirat, ca să știu că trebuie să mă reautentific.
Criterii de acceptare:
- Given tokenul meu a expirat, When fac o acțiune în portal, Then primesc 401 și sunt redirecționat spre login.

## Roluri și autorizare

**US-03 — Roluri distincte USER / OPERATOR / ADMIN**
Ca ADMIN, vreau să pot atribui rolurile USER, OPERATOR sau ADMIN unui membru al echipei, ca să controlez ce poate face fiecare în platformă.
Criterii de acceptare:
- Given sunt ADMIN, When accesez pagina de gestionare roluri, Then pot atribui/schimba rolul unui utilizator.
- Given nu sunt ADMIN, When încerc să accesez pagina de roluri, Then primesc 403.

**US-04 — Permisiuni pe rol**
Ca OPERATOR, vreau să pot face update, rollback și vizualizare a mediilor, dar nu vreau să pot gestiona utilizatori/roluri, ca să respect principiul minimului privilegiu.
Criterii de acceptare:
- Given sunt OPERATOR, When apelez PATCH /api/environments/{id}, Then acțiunea reușește.
- Given sunt OPERATOR, When apelez endpoint-uri de administrare utilizatori, Then primesc 403.

**US-05 — Vizualizare read-only**
Ca USER, vreau să pot vedea starea mediilor și istoricul de deployment, dar fără să pot modifica nimic, ca să am vizibilitate fără risc de acțiuni greșite.
Criterii de acceptare:
- Given sunt USER, When accesez GET /api/environments, Then primesc lista mediilor.
- Given sunt USER, When încerc PATCH/DELETE pe un environment, Then primesc 403.

## Audit

**US-06 — Istoric de audit pentru acțiuni sensibile**
Ca ADMIN, vreau să văd cine a făcut o acțiune (update, delete, rollback) și când, ca să pot investiga incidente sau acțiuni neautorizate.
Criterii de acceptare:
- Given o acțiune de tip update/delete/rollback are loc, When verific audit log-ul, Then văd actorul, timestamp-ul, acțiunea și rezultatul.
- Given caut audit log-ul unui environment specific, When filtrez după ID, Then văd doar evenimentele relevante.

**US-07 — Acces limitat la audit log**
Ca OPERATOR, nu vreau să pot șterge sau modifica intrări din audit log, ca integritatea istoricului să fie garantată.
Criterii de acceptare:
- Given sunt OPERATOR sau ADMIN, When încerc să modific/șterg o intrare de audit, Then request-ul este respins (audit log e append-only).

## Cazuri de eroare / securitate

**US-08 — Acces neautorizat**
Ca sistem, vreau să resping orice request fără token valid sau cu permisiuni insuficiente, ca să protejez resursele platformei.
Criterii de acceptare:
- Given lipsește tokenul, When fac orice request protejat, Then primesc 401.
- Given tokenul e valid dar rolul nu are dreptul necesar, When fac request-ul, Then primesc 403.

**US-09 — Identitate indisponibilă**
Ca utilizator, vreau să primesc un mesaj clar dacă serviciul de identitate (Entra ID) e temporar indisponibil, ca să înțeleg că nu e o eroare din partea mea.
Criterii de acceptare:
- Given Entra ID nu răspunde, When încerc să mă autentific, Then văd un mesaj de eroare specific ("serviciu indisponibil, reîncearcă mai târziu"), nu o eroare generică 500.
