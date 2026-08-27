package com.envforge.controlapi.monitoring;

import static org.hamcrest.Matchers.nullValue;
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
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUserProvider;

@WebMvcTest(MonitoringController.class)
class MonitoringEdgeCasesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringService monitoringService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void shouldReturnPartialDegradedMetrics()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        MetricResponse response = new MetricResponse(
            environmentId,
            "partial-demo",
            "env-partial-demo",
            HealthStatus.DEGRADED,
            null,
            null,
            5.2,
            null,
            Instant.parse("2026-07-30T10:45:00Z")
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
                    .value("partial-demo")
            )
            .andExpect(
                jsonPath("$.namespace")
                    .value("env-partial-demo")
            )
            .andExpect(
                jsonPath("$.status")
                    .value("DEGRADED")
            )
            .andExpect(
                jsonPath("$.cpuUsagePercent")
                    .value(nullValue())
            )
            .andExpect(
                jsonPath("$.memoryUsageBytes")
                    .value(nullValue())
            )
            .andExpect(
                jsonPath("$.requestRatePerSecond")
                    .value(5.2)
            )
            .andExpect(
                jsonPath("$.errorRatePercent")
                    .value(nullValue())
            )
            .andExpect(
                jsonPath("$.capturedAt")
                    .value(
                        "2026-07-30T10:45:00Z"
                    )
            );
    }

    @Test
    void shouldReturnEmptyEventList()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        when(
            monitoringService.findEvents(environmentId)
        ).thenReturn(List.of());

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/events",
                    environmentId
                )
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.length()").value(0)
            );
    }
}
