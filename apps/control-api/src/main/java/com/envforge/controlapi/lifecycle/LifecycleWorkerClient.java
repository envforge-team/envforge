package com.envforge.controlapi.lifecycle;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class LifecycleWorkerClient {

    private final RestClient restClient;

    public LifecycleWorkerClient(
        @Value(
            "${envforge.lifecycle.worker-base-url:http://localhost:8081}"
        )
        String workerBaseUrl,
        @Value(
            "${envforge.lifecycle.internal-token:local-dev-internal-token}"
        )
        String internalToken
    ) {
        this.restClient = RestClient.builder()
            .baseUrl(stripTrailingSlash(workerBaseUrl))
            .defaultHeader(
                "X-EnvForge-Internal-Token",
                internalToken
            )
            .build();
    }

    public LifecycleJobResponse createJob(
        UUID environmentId,
        LifecycleAction action,
        Integer targetRevision,
        String actorId,
        String namespaceName,
        String helmReleaseName
    ) {
        CreateLifecycleJobRequest request =
            new CreateLifecycleJobRequest(
                environmentId,
                action.name(),
                targetRevision,
                actorId,
                namespaceName,
                helmReleaseName
            );

        try {
            LifecycleJobResponse response =
                restClient.post()
                    .uri("/internal/lifecycle/jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(LifecycleJobResponse.class);

            if (response == null) {
                throw new LifecycleWorkerException(
                    502,
                    "cleanup-worker returned an empty response"
                );
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new LifecycleWorkerException(
                exception.getStatusCode().value(),
                "cleanup-worker rejected lifecycle request: "
                    + exception.getResponseBodyAsString(),
                exception
            );
        } catch (RestClientException exception) {
            throw new LifecycleWorkerException(
                503,
                "Could not reach cleanup-worker",
                exception
            );
        }
    }

    private String stripTrailingSlash(String value) {
        String result = value;

        while (result.endsWith("/")) {
            result = result.substring(
                0,
                result.length() - 1
            );
        }

        return result;
    }

    private record CreateLifecycleJobRequest(
        UUID environmentId,
        String action,
        Integer targetRevision,
        String actorId,
        String namespaceName,
        String helmReleaseName
    ) {
    }
}
