package com.envforge.controlapi.monitoring;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;
import com.envforge.controlapi.user.Role;

@RestController
@RequestMapping(
    "/api/environments/{environmentId}/monitoring"
)
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    public MonitoringController(
        MonitoringService monitoringService,
        CurrentUserProvider currentUserProvider,
        AuthorizationService authorizationService
    ) {
        this.monitoringService = monitoringService;
        this.currentUserProvider = currentUserProvider;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricResponse> findLatestMetrics(
        @PathVariable UUID environmentId
    ) {
        requireMonitoringAccess("VIEW_MONITORING_METRICS");

        Optional<MetricResponse> metrics =
            monitoringService.findLatestMetrics(
                environmentId
            );

        if (metrics.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(metrics.orElseThrow());
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> findEvents(
        @PathVariable UUID environmentId
    ) {
        requireMonitoringAccess("VIEW_MONITORING_EVENTS");

        return ResponseEntity.ok(
            monitoringService.findEvents(environmentId)
        );
    }

    private void requireMonitoringAccess(String action) {
        CurrentUser currentUser =
            currentUserProvider.getCurrentUser();

        authorizationService.requireRole(
            currentUser,
            action,
            Role.OPERATOR,
            Role.ADMIN
        );
    }
}
