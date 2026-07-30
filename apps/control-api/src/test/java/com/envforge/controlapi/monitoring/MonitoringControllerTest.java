package com.envforge.controlapi.monitoring;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test
    .autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override
    .mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.envforge.controlapi.environment
    .EnvironmentNotFoundException;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringService monitoringService;

    @Test
    void shouldReturnLatestMetrics() throws Exception {
        UUID environmentId = UUID.randomUUID();

        MetricResponse response = new MetricResponse(
            environmentId,
            "monitoring-demo",
            "env-monitoring-demo",
            HealthStatus.HEALTHY,
            23.7,
            384_827_392L,
            18.4,
            0.6,
            Instant.parse("2026-07-30T10:40:00Z")
        );

        when(
            monitoringService.findLatestMetrics(
                environmentId
            )
        ).thenReturn(Optional.of(response));

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/metrics",
                    environmentId
                )
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.environmentId")
                    .value(environmentId.toString())
            )
            .andExpect(
                jsonPath("$.environmentName")
                    .value("monitoring-demo")
            )
            .andExpect(
                jsonPath("$.namespace")
                    .value("env-monitoring-demo")
            )
            .andExpect(
                jsonPath("$.status")
                    .value("HEALTHY")
            )
            .andExpect(
                jsonPath("$.cpuUsagePercent")
                    .value(23.7)
            )
            .andExpect(
                jsonPath("$.memoryUsageBytes")
                    .value(384827392)
            );
    }

    @Test
    void shouldReturnNoContentWhenNoSnapshotExists()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        when(
            monitoringService.findLatestMetrics(
                environmentId
            )
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/metrics",
                    environmentId
                )
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnEnvironmentEvents()
        throws Exception {

        UUID environmentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        EventResponse response = new EventResponse(
            eventId,
            environmentId,
            EnvironmentEventType.POD_RESTARTED,
            EventSeverity.WARNING,
            "kubernetes",
            "The workload pod restarted.",
            Instant.parse("2026-07-30T10:39:12Z")
        );

        when(
            monitoringService.findEvents(environmentId)
        ).thenReturn(List.of(response));

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/events",
                    environmentId
                )
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.length()").value(1)
            )
            .andExpect(
                jsonPath("$[0].id")
                    .value(eventId.toString())
            )
            .andExpect(
                jsonPath("$[0].eventType")
                    .value("POD_RESTARTED")
            )
            .andExpect(
                jsonPath("$[0].severity")
                    .value("WARNING")
            )
            .andExpect(
                jsonPath("$[0].source")
                    .value("kubernetes")
            );
    }

    @Test
    void shouldReturnNotFoundWhenEnvironmentIsMissing()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        when(
            monitoringService.findLatestMetrics(
                environmentId
            )
        ).thenThrow(
            new EnvironmentNotFoundException(
                environmentId
            )
        );

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/metrics",
                    environmentId
                )
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.status").value(404)
            )
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Environment not found: "
                            + environmentId
                    )
            );
    }
}
