FROM nginxinc/nginx-unprivileged:alpine

LABEL org.opencontainers.image.title="EnvForge Static Web Demo"
LABEL org.opencontainers.image.description="Static web workload template for EnvForge"
LABEL org.opencontainers.image.vendor="EnvForge Team"
LABEL org.opencontainers.image.version="0.2.0"
LABEL org.opencontainers.image.source="https://github.com/envforge-team/envforge"

COPY --chown=101:101 nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=101:101 index.html /usr/share/nginx/html/index.html

USER 101

EXPOSE 8080

HEALTHCHECK \
  --interval=10s \
  --timeout=3s \
  --start-period=5s \
  --retries=3 \
  CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8080/health"]