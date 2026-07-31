# EnvForge Static Web Demo

A minimal Nginx workload used as an EnvForge environment template.

The container:

- runs as a non-root user;
- listens on port 8080;
- exposes a health endpoint;
- can be deployed through Kubernetes and Helm.

## Build

```bash
docker build \
  --tag envforge/static-web-demo:0.2.0 \
  .
```

## Run

```bash
docker run --rm -d \
  --name envforge-static-web \
  --publish 8080:8080 \
  envforge/static-web-demo:0.2.0
```

## Verify

```bash
curl http://localhost:8080/
curl http://localhost:8080/health
```

## Check health

```bash
docker inspect \
  --format='{{.State.Health.Status}}' \
  envforge-static-web
```

## Stop

```bash
docker stop envforge-static-web
```