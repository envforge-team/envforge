FROM python:3.13-alpine

WORKDIR /app

RUN addgroup -S envforge \
    && adduser -S -G envforge envforge

COPY generator.py .

USER envforge

ENV TARGET_URL=http://reliability-demo-api/work
ENV REQUESTS_PER_SECOND=2
ENV REQUEST_TIMEOUT_SECONDS=5

CMD ["python", "-u", "generator.py"]
