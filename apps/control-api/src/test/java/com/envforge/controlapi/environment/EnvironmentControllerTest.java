package com.envforge.controlapi.environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test
    .autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override
    .mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(EnvironmentController.class)
class EnvironmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvironmentService environmentService;

    @Test
    void shouldCreateEnvironment() throws Exception {
        UUID id = UUID.randomUUID();

        EnvironmentResponse response =
            TestEnvironmentFactory.response(
                id,
                "controller-demo"
            );

        when(
            environmentService.create(
                any(CreateEnvironmentRequest.class)
            )
        ).thenReturn(response);

        String request = """
            {
              "name": "controller-demo",
              "template": "STATIC_WEB",
              "imageVersion": "0.1.0",
              "replicas": 2,
              "resourceProfile": "SMALL",
              "lifetimeHours": 4,
              "monitoringEnabled": true
            }
            """;

        mockMvc.perform(
                post("/api/environments")
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content(request)
            )
            .andExpect(status().isCreated())
            .andExpect(
                header().string(
                    "Location",
                    "/api/environments/" + id
                )
            )
            .andExpect(
                jsonPath("$.id")
                    .value(id.toString())
            )
            .andExpect(
                jsonPath("$.name")
                    .value("controller-demo")
            )
            .andExpect(
                jsonPath("$.namespace")
                    .value("env-controller-demo")
            )
            .andExpect(
                jsonPath("$.status")
                    .value("REQUESTED")
            );
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        String request = """
            {
              "name": "INVALID NAME",
              "template": "STATIC_WEB",
              "imageVersion": "",
              "replicas": 10,
              "resourceProfile": "SMALL",
              "lifetimeHours": 100,
              "monitoringEnabled": true
            }
            """;

        mockMvc.perform(
                post("/api/environments")
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content(request)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.status").value(400)
            )
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Request validation failed"
                    )
            )
            .andExpect(
                jsonPath(
                    "$.validationErrors.name"
                ).exists()
            )
            .andExpect(
                jsonPath(
                    "$.validationErrors.imageVersion"
                ).exists()
            )
            .andExpect(
                jsonPath(
                    "$.validationErrors.replicas"
                ).exists()
            )
            .andExpect(
                jsonPath(
                    "$.validationErrors.lifetimeHours"
                ).exists()
            );
    }

    @Test
    void shouldListEnvironments() throws Exception {
        EnvironmentResponse first =
            TestEnvironmentFactory.response(
                UUID.randomUUID(),
                "first-demo"
            );

        EnvironmentResponse second =
            TestEnvironmentFactory.response(
                UUID.randomUUID(),
                "second-demo"
            );

        when(environmentService.findAll())
            .thenReturn(List.of(first, second));

        mockMvc.perform(
                get("/api/environments")
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.length()").value(2)
            )
            .andExpect(
                jsonPath("$[0].name")
                    .value("first-demo")
            )
            .andExpect(
                jsonPath("$[1].name")
                    .value("second-demo")
            );
    }

    @Test
    void shouldFindEnvironmentById() throws Exception {
        UUID id = UUID.randomUUID();

        when(environmentService.findById(id))
            .thenReturn(
                TestEnvironmentFactory.response(
                    id,
                    "details-demo"
                )
            );

        mockMvc.perform(
                get("/api/environments/{id}", id)
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(id.toString())
            )
            .andExpect(
                jsonPath("$.name")
                    .value("details-demo")
            );
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        when(environmentService.findById(id))
            .thenThrow(
                new EnvironmentNotFoundException(id)
            );

        mockMvc.perform(
                get("/api/environments/{id}", id)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.status").value(404)
            )
            .andExpect(
                jsonPath("$.error")
                    .value("Not Found")
            )
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Environment not found: " + id
                    )
            );
    }
}