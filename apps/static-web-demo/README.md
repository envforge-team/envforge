# EnvForge Static Web Demo

A minimal Nginx application used as an EnvForge environment template.

## Build

```bash
docker build -t envforge/static-web-demo:0.1.0 .
```

## Run

```bash
docker run --rm \
  --name envforge-static-web \
  -p 8080:80 \
  envforge/static-web-demo:0.1.0
```

## Verify

Open:

```text
http://localhost:8080
```

Or run:

```bash
curl http://localhost:8080
```

## Stop

Press `Ctrl+C` or run:

```bash
docker stop envforge-static-web
```