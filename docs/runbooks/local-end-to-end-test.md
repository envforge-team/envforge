# Local end-to-end test

This runbook verifies the EnvForge local flow:

```text
PostgreSQL
→ Control API
→ Template API
→ Environment creation
→ Environment query
```

## Prerequisites

The following tools must be available:

- Docker;
- Docker Compose;
- Java 21;
- curl;
- jq.

## Start PostgreSQL

From the repository root:

```bash
docker compose up -d postgres
docker compose ps
```

PostgreSQL must report a healthy status.

## Start Control API

```bash
cd apps/control-api
./mvnw spring-boot:run
```

Verify:

```bash
curl http://localhost:8080/actuator/health
```

## Start the portal

In another terminal:

```bash
cd apps/portal
npm run dev
```

Open:

```text
http://localhost:5173
```

## Run the automated smoke test

From the repository root:

```bash
./scripts/smoke-test-local.sh
```

The script verifies:

1. Control API health;
2. availability of templates;
3. environment creation;
4. generated namespace;
5. initial `REQUESTED` status;
6. environment details;
7. environment listing.

## Expected result

```text
Smoke test passed.
```

## Troubleshooting

### PostgreSQL is unavailable

```bash
docker compose ps
docker compose logs postgres
```

### Control API is unavailable

```bash
curl http://localhost:8080/actuator/health
```

### Port 5432 is already in use

Check the process or container:

```bash
docker ps
ss -ltnp | grep 5432
```

### Port 8080 is already in use

```bash
ss -ltnp | grep 8080
```

### Portal cannot load templates

Verify:

```bash
curl http://localhost:8080/api/templates
```

Also verify the Vite proxy configuration.