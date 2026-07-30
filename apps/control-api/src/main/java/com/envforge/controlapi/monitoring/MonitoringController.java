package com.envforge.controlapi.monitoring;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/environments/{environmentId}/monitoring"
)
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(
        MonitoringService monitoringService
    ) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricResponse> findLatestMetrics(
        @PathVariable UUID environmentId
    ) {
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
        return ResponseEntity.ok(
            monitoringService.findEvents(environmentId)
        );
    }
}
