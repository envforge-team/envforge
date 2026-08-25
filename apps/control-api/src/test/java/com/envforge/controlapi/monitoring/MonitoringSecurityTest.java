package com.envforge.controlapi.monitoring;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override
    .mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.envforge.controlapi.security.EntraSecurityConfig;

@WebMvcTest(MonitoringController.class)
@ActiveProfiles("entra")
@Import(EntraSecurityConfig.class)
class MonitoringSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringService monitoringService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void metricsShouldRejectUnauthenticatedRequests()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/metrics",
                    environmentId
                )
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void eventsShouldRejectUnauthenticatedRequests()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/events",
                    environmentId
                )
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsShouldAllowAuthenticatedRequests()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        when(jwtDecoder.decode("valid-token"))
            .thenReturn(testJwt());

        when(
            monitoringService.findLatestMetrics(environmentId)
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/metrics",
                    environmentId
                )
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer valid-token"
                    )
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void eventsShouldAllowAuthenticatedRequests()
        throws Exception {

        UUID environmentId = UUID.randomUUID();

        when(jwtDecoder.decode("valid-token"))
            .thenReturn(testJwt());

        when(
            monitoringService.findEvents(environmentId)
        ).thenReturn(List.of());

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/events",
                    environmentId
                )
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer valid-token"
                    )
            )
            .andExpect(status().isOk());
    }

    private Jwt testJwt() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("valid-token")
            .header("alg", "RS256")
            .claim("sub", "monitoring-user")
            .claim(
                "preferred_username",
                "monitoring-user@envforge.local"
            )
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .build();
    }
}
