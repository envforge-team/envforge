# EnvForge – Self-Service AKS Sandbox Platform

## Descriere generală

EnvForge este o platformă DevOps care permite crearea, administrarea și ștergerea automată a mediilor temporare (sandbox environments) în Azure Kubernetes Service (AKS).

Scopul proiectului este să simulăm modul în care funcționează o echipă de Platform Engineering dintr-o companie reală, unde dezvoltatorii își pot crea singuri medii de test fără să configureze manual Kubernetes sau infrastructura.

Platforma va automatiza întregul ciclu de viață al unui mediu:

* crearea unui mediu nou;
* configurarea resurselor necesare;
* deployment-ul aplicației în AKS;
* monitorizarea aplicației și a infrastructurii;
* actualizarea aplicației;
* rollback la o versiune anterioară;
* ștergerea automată sau manuală a mediului.

## Cum funcționează

Utilizatorul completează un formular sau lansează un workflow în care specifică:

* numele mediului;
* template-ul aplicației;
* versiunea imaginii Docker;
* numărul de replici;
* profilul de resurse (Small, Medium, Large);
* durata de viață a mediului.

Platforma execută automat următoarele operații:

1. validează cererea;
2. pornește pipeline-ul CI/CD;
3. preia imaginea din Azure Container Registry (ACR);
4. creează un namespace dedicat în AKS;
5. instalează aplicația folosind Helm;
6. verifică dacă deployment-ul este sănătos;
7. configurează monitorizarea;
8. pune la dispoziție URL-ul aplicației;
9. șterge mediul la cerere sau după expirarea timpului stabilit.

## Tehnologii utilizate

* Azure Kubernetes Service (AKS)
* Terraform
* Docker
* Kubernetes
* Helm
* GitHub Actions (sau Azure DevOps)
* Azure Container Registry (ACR)
* Azure Monitor
* Prometheus
* Grafana
* Spring Boot (Control API)
* React (Portal)
* PostgreSQL

## Scopul proiectului

Acest proiect nu urmărește dezvoltarea unei aplicații complexe, ci construirea unei platforme DevOps complete care demonstrează:

* Infrastructure as Code;
* containerizare;
* orchestrare Kubernetes;
* automatizarea deployment-urilor;
* CI/CD;
* observability și monitoring;
* managementul ciclului de viață al mediilor;
* bune practici de Platform Engineering.

## Obiectivul final

La finalul proiectului vom putea demonstra un flux complet:

* utilizatorul solicită un mediu nou;
* infrastructura și aplicația sunt create automat;
* aplicația devine accesibilă printr-un URL;
* mediul este monitorizat în Grafana și Azure Monitor;
* poate fi actualizat sau restaurat prin rollback;
* este eliminat automat la expirare sau manual la cerere.

Proiectul este gândit astfel încât fiecare membru al echipei să contribuie atât la partea de dezvoltare software, cât și la partea de infrastructură, Kubernetes, Terraform, CI/CD și monitorizare, pentru ca responsabilitățile și experiența dobândită să fie distribuite cât mai echilibrat.


## Documentație operațională

* [Create an Environment](docs/create-an-environment.md)
* [Provisioning Troubleshooting](docs/provisioning-troubleshooting.md)