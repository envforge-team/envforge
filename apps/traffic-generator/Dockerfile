FROM python:3.14-alpine

WORKDIR /app

RUN addgroup -S -g 10001 envforge \
    && adduser -S -D -H -u 10001 -G envforge envforge

COPY generator.py .

USER 10001:10001

ENV TARGET_URL=http://reliability-demo-api/work
ENV REQUESTS_PER_SECOND=2
ENV REQUEST_TIMEOUT_SECONDS=5

CMD ["python", "-u", "generator.py"]
