package com.envforge.controlapi.monitoring;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test
    .autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override
    .mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import com.envforge.controlapi.security.SecurityMetrics;
import com.envforge.controlapi.user.Role;

@WebMvcTest(MonitoringController.class)
@Import(AuthorizationService.class)
class MonitoringAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringService monitoringService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private SecurityMetrics securityMetrics;

    @Test
    void userShouldNotViewMetrics() throws Exception {
        UUID environmentId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser())
            .thenReturn(user(Role.USER));

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/metrics",
                    environmentId
                )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void userShouldNotViewEvents() throws Exception {
        UUID environmentId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser())
            .thenReturn(user(Role.USER));

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/events",
                    environmentId
                )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void operatorShouldViewMetrics() throws Exception {
        UUID environmentId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser())
            .thenReturn(user(Role.OPERATOR));

        when(
            monitoringService.findLatestMetrics(environmentId)
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
    void operatorShouldViewEvents() throws Exception {
        UUID environmentId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser())
            .thenReturn(user(Role.OPERATOR));

        when(
            monitoringService.findEvents(environmentId)
        ).thenReturn(List.of());

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/events",
                    environmentId
                )
            )
            .andExpect(status().isOk());
    }

    @Test
    void adminShouldViewMetrics() throws Exception {
        UUID environmentId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser())
            .thenReturn(user(Role.ADMIN));

        when(
            monitoringService.findLatestMetrics(environmentId)
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
    void adminShouldViewEvents() throws Exception {
        UUID environmentId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser())
            .thenReturn(user(Role.ADMIN));

        when(
            monitoringService.findEvents(environmentId)
        ).thenReturn(List.of());

        mockMvc.perform(
                get(
                    "/api/environments/{environmentId}/monitoring/events",
                    environmentId
                )
            )
            .andExpect(status().isOk());
    }

    private CurrentUser user(Role role) {
        return new CurrentUser(
            role.name().toLowerCase() + "-1",
            role.name().toLowerCase() + "@envforge.local",
            role.name(),
            role
        );
    }
}
