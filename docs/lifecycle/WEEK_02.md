# Week 2 - Lifecycle Domain and Worker Skeleton

## Objective

The objective of Week 2 is to create the initial Spring Boot cleanup-worker application and implement the lifecycle domain rules from the state machine.

No real Helm or Kubernetes command is executed during this week.

## Deliverables

- Spring Boot project configuration;
- lifecycle configuration properties;
- environment status enum;
- lifecycle action enum;
- lifecycle job status enum;
- lifecycle job model;
- environment lifecycle context;
- transition validator;
- lifecycle decision service;
- command runner interface;
- dry-run command runner;
- unit tests.

## Day 1 - Spring Boot skeleton

Add:

```text
apps/cleanup-worker/pom.xml
apps/cleanup-worker/src/main/resources/application.yml
apps/cleanup-worker/src/main/java/com/envforge/cleanupworker/CleanupWorkerApplication.java
```

Validation:

```bash
cd apps/cleanup-worker
mvn test
```

The application must compile and the Spring Boot context must be available.

## Day 2 - Lifecycle domain

Add:

```text
EnvironmentStatus.java
LifecycleAction.java
LifecycleJobStatus.java
LifecycleJob.java
EnvironmentLifecycleContext.java
```

The enum values must match `STATE_MACHINE.md`.

## Day 3 - Transition validation

Add:

```text
InvalidLifecycleTransitionException.java
LifecycleTransitionValidator.java
LifecycleDecisionService.java
```

The validator must reject invalid transitions such as:

```text
DELETED -> READY
DELETING -> UPDATING
ROLLING_BACK -> UPDATING
```

Rollback must require a previous successful Helm revision.

## Day 4 - Command abstraction

Add:

```text
LifecycleCommandRunner.java
CommandResult.java
DryRunLifecycleCommandRunner.java
```

The dry-run runner must not execute:

```text
helm uninstall
helm rollback
kubectl delete
```

It only returns the command that would have been executed.

## Day 5 - Tests and review

Run:

```bash
cd apps/cleanup-worker
mvn clean test
```

Expected result:

```text
BUILD SUCCESS
```

Review with the team:

- package naming;
- Java and Spring Boot versions;
- final environment statuses;
- ownership of rollback execution;
- required database models;
- required audit fields;
- whether the worker remains a Spring Boot application.

## Week 2 Definition of Done

- [ ] Cleanup worker compiles.
- [ ] All tests pass.
- [ ] Status enum matches the state machine.
- [ ] Invalid transitions are rejected.
- [ ] Rollback requires a previous revision.
- [ ] Command execution is abstracted.
- [ ] Only dry-run command execution exists.
- [ ] No destructive Kubernetes or Helm operation is enabled.
