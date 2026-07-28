# EnvForge Cleanup Worker

Cleanup Worker is the component responsible for lifecycle operations for EnvForge environments.

## Current Week 2 functionality

The current implementation contains:

- Spring Boot application skeleton;
- lifecycle configuration;
- environment status model;
- lifecycle job model;
- state transition validation;
- lifecycle decision service;
- Helm and Kubernetes command abstraction;
- dry-run command runner;
- unit tests.

## Safety

The current implementation does not execute real Helm or Kubernetes commands.

`DryRunLifecycleCommandRunner` only returns text describing the command that would have been executed.

## Run tests

From Git Bash:

```bash
cd apps/cleanup-worker
mvn clean test
```

From PowerShell:

```powershell
Set-Location apps\cleanup-worker
mvn clean test
```

## Run application

```bash
mvn spring-boot:run
```

Health endpoint:

```text
http://localhost:8080/actuator/health
```
