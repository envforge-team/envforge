FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy

ARG HELM_VERSION=v3.18.4
ARG KUBECTL_VERSION=v1.32.2
ARG TARGETARCH=amd64

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        tar \
    && curl -fsSL \
        "https://get.helm.sh/helm-${HELM_VERSION}-linux-${TARGETARCH}.tar.gz" \
        -o /tmp/helm.tar.gz \
    && tar -xzf /tmp/helm.tar.gz -C /tmp \
    && mv "/tmp/linux-${TARGETARCH}/helm" /usr/local/bin/helm \
    && chmod +x /usr/local/bin/helm \
    && curl -fsSL \
        "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${TARGETARCH}/kubectl" \
        -o /usr/local/bin/kubectl \
    && chmod +x /usr/local/bin/kubectl \
    && rm -rf \
        /var/lib/apt/lists/* \
        /tmp/helm.tar.gz \
        "/tmp/linux-${TARGETARCH}"

RUN useradd \
    --system \
    --uid 10001 \
    --create-home \
    --home-dir /home/envforge \
    envforge

WORKDIR /app

COPY --from=build \
    /workspace/target/cleanup-worker-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER 10001:10001

ENV HOME=/home/envforge

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
